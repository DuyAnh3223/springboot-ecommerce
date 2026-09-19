package spring.abtechzone.modules.shipment.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnFeeRequest;

/** Narrow GHN boundary: only the fee endpoint is used by ABTechZone. */
@Component
@RequiredArgsConstructor
public class GhnFeeClient {
    private final GhnShippingProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public int calculateFee(GhnFeeRequest request) {
        if (!properties.isEnabled()) {
            throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
        }
        if (properties.getToken() == null || properties.getToken().isBlank() || properties.getShopId() <= 0) {
            throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
        }
        try {
            String body = objectMapper.writeValueAsString(requestBody(request));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/v2/shipping-order/fee"))
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Token", properties.getToken())
                    .header("ShopId", String.valueOf(properties.getShopId()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("code").asInt() != 200 || !root.path("data").isObject()) {
                throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
            }
            int total = root.path("data").path("total").asInt(-1);
            if (total < 0) {
                throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
            }
            return total;
        } catch (AppException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
        } catch (Exception e) {
            throw new AppException(ErrorCode.SHIPPING_PROVIDER_UNAVAILABLE);
        }
    }

    private java.util.Map<String, Object> requestBody(GhnFeeRequest request) {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("from_district_id", request.fromDistrictId());
        body.put("from_ward_code", request.fromWardCode());
        body.put("to_district_id", request.toDistrictId());
        body.put("to_ward_code", request.toWardCode());
        body.put("service_type_id", request.serviceTypeId());
        body.put("weight", request.weight());
        return body;
    }
}
