package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import spring.abtechzone.modules.payment.gateway.*;

class GatewayHttpTest {
    @Test
    void sendsJsonAndHeadersThroughRealHttpAndRejectsRedirects() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var observed = new AtomicReference<String>();
        server.createContext("/create", exchange -> {
            observed.set(exchange.getRequestHeaders().getFirst("x-client-id") + ":"
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"code\":\"00\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", "/create");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var transport = new GatewayHttp();
            assertThat(transport
                            .exchange(
                                    base + "/create",
                                    Map.of("x-client-id", "test"),
                                    GatewayCrypto.JSON.readTree("{\"amount\":10000}"))
                            .path("code")
                            .asText())
                    .isEqualTo("00");
            assertThat(observed.get()).isEqualTo("test:{\"amount\":10000}");
            assertThatThrownBy(() -> transport.exchange(base + "/redirect", Map.of(), null))
                    .isInstanceOf(GatewayException.class);
        } finally {
            server.stop(0);
        }
    }
}
