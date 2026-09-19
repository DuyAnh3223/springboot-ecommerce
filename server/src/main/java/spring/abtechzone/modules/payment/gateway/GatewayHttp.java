package spring.abtechzone.modules.payment.gateway;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class GatewayHttp {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public JsonNode exchange(String url, Map<String, String> headers, JsonNode body) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(35))
                    .header("Accept", "application/json");
            headers.forEach(request::header);
            if (body == null) request.GET();
            else
                request.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            var pending = client.sendAsync(request.build(), info -> limitedBody());
            java.net.http.HttpResponse<String> response;
            try {
                response = pending.get(35, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                pending.cancel(true);
                throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
            }
            if (response.statusCode() != 200 || response.body().length() > 65536)
                throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
            return GatewayCrypto.JSON.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
        } catch (GatewayException e) {
            throw e;
        } catch (Exception e) {
            throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
        }
    }

    private HttpResponse.BodySubscriber<String> limitedBody() {
        var downstream = HttpResponse.BodySubscribers.ofString(java.nio.charset.StandardCharsets.UTF_8);
        return new HttpResponse.BodySubscriber<>() {
            private java.util.concurrent.Flow.Subscription subscription;
            private long bytes;
            private boolean stopped;

            public java.util.concurrent.CompletionStage<String> getBody() {
                return downstream.getBody();
            }

            public void onSubscribe(java.util.concurrent.Flow.Subscription value) {
                subscription = value;
                downstream.onSubscribe(value);
            }

            public void onNext(java.util.List<java.nio.ByteBuffer> buffers) {
                if (stopped) return;
                for (var buffer : buffers) bytes += buffer.remaining();
                if (bytes > 65536) {
                    stopped = true;
                    subscription.cancel();
                    downstream.onError(new GatewayException(GatewayException.Kind.UNAVAILABLE));
                } else downstream.onNext(buffers);
            }

            public void onError(Throwable error) {
                if (!stopped) downstream.onError(error);
            }

            public void onComplete() {
                if (!stopped) downstream.onComplete();
            }
        };
    }
}
