package spring.abtechzone.modules.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.client.GhnFeeClient;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnFeeRequest;

class GhnFeeClientTest {

    private HttpServer server;
    private GhnShippingProperties properties;
    private GhnFeeClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        properties = new GhnShippingProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setToken("test-token");
        properties.setShopId(12345);
        properties.setConnectTimeoutSeconds(2);
        properties.setRequestTimeoutSeconds(2);
        client = new GhnFeeClient(properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void calculateFee_sendsRouteWeightAndCredentialsAndParsesTotal() {
        AtomicReference<String> body = new AtomicReference<>();
        server.createContext("/v2/shipping-order/fee", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            assertThat(exchange.getRequestHeaders().getFirst("Token")).isEqualTo("test-token");
            assertThat(exchange.getRequestHeaders().getFirst("ShopId")).isEqualTo("12345");
            writeResponse(exchange, 200, "{\"code\":200,\"data\":{\"total\":42000}}");
        });
        server.start();

        int fee = client.calculateFee(new GhnFeeRequest(1450, "21008", 1442, "20308", 2, 500));

        assertThat(fee).isEqualTo(42000);
        assertThat(body)
                .hasValue(
                        "{\"from_district_id\":1450,\"from_ward_code\":\"21008\",\"to_district_id\":1442,\"to_ward_code\":\"20308\",\"service_type_id\":2,\"weight\":500}");
    }

    @Test
    void calculateFee_mapsProviderFailureToUnavailable() {
        server.createContext(
                "/v2/shipping-order/fee",
                exchange -> writeResponse(exchange, 200, "{\"code\":400,\"message\":\"invalid route\"}"));
        server.start();

        assertThatThrownBy(() -> client.calculateFee(new GhnFeeRequest(1450, "21008", 1442, "20308", 2, 500)))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE.getMessage());
    }

    private static void writeResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
