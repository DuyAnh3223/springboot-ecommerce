package spring.abtechzone.modules.shipment.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnDistrictData;
import spring.abtechzone.modules.shipment.dto.GhnProvinceData;
import spring.abtechzone.modules.shipment.dto.GhnWardData;

/** Server-only boundary for the GHN master-data catalogue. */
@Component
@RequiredArgsConstructor
public class GhnLocationClient {
    private final GhnShippingProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<GhnProvinceData> getProvinces() {
        return getArray("/master-data/province", null).stream()
                .map(node -> new GhnProvinceData(
                        requiredInt(node, "ProvinceID", "province_id"),
                        requiredText(node, "ProvinceName", "province_name")))
                .toList();
    }

    public List<GhnDistrictData> getDistricts(int provinceId) {
        return getArray("/master-data/district", "province_id=" + provinceId).stream()
                .map(node -> new GhnDistrictData(
                        requiredInt(node, "DistrictID", "district_id"),
                        requiredText(node, "DistrictName", "district_name"),
                        optionalInt(node, "SupportType", "support_type")))
                .toList();
    }

    public List<GhnWardData> getWards(int districtId) {
        return getArray("/master-data/ward", "district_id=" + districtId).stream()
                .map(node -> new GhnWardData(
                        requiredText(node, "WardCode", "ward_code"),
                        requiredText(node, "WardName", "ward_name"),
                        optionalInt(node, "SupportType", "support_type")))
                .toList();
    }

    private List<JsonNode> getArray(String path, String query) {
        if (!properties.isEnabled()
                || properties.getToken() == null
                || properties.getToken().isBlank()
                || properties.getShopId() <= 0) {
            throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
        }
        try {
            String url = trimTrailingSlash(properties.getBaseUrl()) + path;
            if (query != null && !query.isBlank()) {
                url += "?" + query;
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .header("Token", properties.getToken())
                    .header("ShopId", String.valueOf(properties.getShopId()))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("code").asInt() != 200 || !root.path("data").isArray()) {
                throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
            }
            List<JsonNode> values = new ArrayList<>();
            root.path("data").forEach(values::add);
            return values;
        } catch (AppException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
        } catch (Exception e) {
            throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
        }
    }

    private int requiredInt(JsonNode node, String... names) {
        Integer value = optionalInt(node, names);
        if (value == null || value <= 0) {
            throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
        }
        return value;
    }

    private Integer optionalInt(JsonNode node, String... names) {
        JsonNode value = find(node, names);
        if (value == null || value.isNull() || (!value.isNumber() && !value.isTextual())) {
            return null;
        }
        try {
            return Integer.valueOf(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String requiredText(JsonNode node, String... names) {
        JsonNode value = find(node, names);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
        }
        return value.asText().trim();
    }

    private JsonNode find(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isMissingNode()) {
                return value;
            }
        }
        return null;
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new AppException(ErrorCode.SHIPPING_LOCATION_UNAVAILABLE);
        }
        return baseUrl.replaceAll("/+$", "");
    }
}
