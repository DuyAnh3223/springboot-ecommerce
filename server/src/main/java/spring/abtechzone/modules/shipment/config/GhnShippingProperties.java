package spring.abtechzone.modules.shipment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/** Configuration for the single, server-managed GHN fee quote. */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.shipping.ghn")
public class GhnShippingProperties {
    private boolean enabled = true;
    private String baseUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";
    private String token = "";
    private int shopId;
    private int fromDistrictId;
    private String fromWardCode = "";
    private int fixedWeightGram = 500;
    private int serviceTypeId = 2;
    private int connectTimeoutSeconds = 5;
    private int requestTimeoutSeconds = 10;
    private long locationCacheTtlSeconds = 86400;
}
