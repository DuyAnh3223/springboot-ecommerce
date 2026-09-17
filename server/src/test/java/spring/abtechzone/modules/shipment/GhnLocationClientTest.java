package spring.abtechzone.modules.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.client.GhnLocationClient;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;

class GhnLocationClientTest {

    private HttpServer server;
    private GhnLocationClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        GhnShippingProperties properties = new GhnShippingProperties();
        properties.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.setToken("test-token");
        properties.setShopId(12345);
        properties.setConnectTimeoutSeconds(2);
        properties.setRequestTimeoutSeconds(2);
        client = new GhnLocationClient(properties);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void getLocations_sendsCredentialsAndParsesLegacyCatalogue() {
        server.createContext("/master-data/province", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Token")).isEqualTo("test-token");
            assertThat(exchange.getRequestHeaders().getFirst("ShopId")).isEqualTo("12345");
            writeResponse(
                    exchange, 200, "{\"code\":200,\"data\":[{\"ProvinceID\":202,\"ProvinceName\":\"Hồ Chí Minh\"}]}");
        });
        server.createContext(
                "/master-data/district",
                exchange -> writeResponse(
                        exchange,
                        200,
                        "{\"code\":200,\"data\":[{\"DistrictID\":1442,\"DistrictName\":\"Quận 1\",\"SupportType\":2}] }"));
        server.createContext(
                "/master-data/ward",
                exchange -> writeResponse(
                        exchange,
                        200,
                        "{\"code\":200,\"data\":[{\"WardCode\":\"20308\",\"WardName\":\"Bến Nghé\",\"SupportType\":3}] }"));
        server.start();

        assertThat(client.getProvinces())
                .extracting("id", "name")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(202, "Hồ Chí Minh"));
        assertThat(client.getDistricts(202).get(0).supportType()).isEqualTo(2);
        assertThat(client.getWards(1442).get(0).code()).isEqualTo("20308");
    }

    @Test
    void getProvinces_mapsProviderFailureToUnavailable() {
        server.createContext(
                "/master-data/province",
                exchange -> writeResponse(exchange, 200, "{\"code\":400,\"message\":\"invalid\"}"));
        server.start();

        assertThatThrownBy(() -> client.getProvinces())
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE.getMessage());
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
