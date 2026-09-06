package spring.abtechzone.modules.payment.config;

import java.net.URI;
import java.time.Clock;
import java.util.*;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.Data;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.payment.constant.PaymentProvider;

@Data
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "app.payment.online")
public class OnlinePaymentProperties {
    private int deadlineMinutes = 15;
    private int maxQueries = 96;
    private int queryWindowHours = 24;
    private String returnBaseUrl = "http://localhost:3000";
    private Map<PaymentProvider, Gateway> gateways = new EnumMap<>(PaymentProvider.class);

    @Data
    @lombok.ToString(onlyExplicitlyIncluded = true)
    public static class Gateway {
        private boolean enabled;
        private String merchantId = "";
        private String accessKey = "";
        private String secretKey = "";
        private String callbackUrl = "";
        private String apiUrl = "";
        private String checkoutUrl = "";
    }

    @Bean
    public Clock paymentClock() {
        return Clock.systemUTC();
    }

    public Gateway require(PaymentProvider provider) {
        Gateway config = provider == null ? null : gateways.get(provider);
        if (config == null || !config.isEnabled()) throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_AVAILABLE);
        return config;
    }

    public List<PaymentProvider> enabledProviders() {
        return gateways.entrySet().stream()
                .filter(e -> e.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    @PostConstruct
    void validate() {
        if (deadlineMinutes < 1
                || deadlineMinutes > 60
                || maxQueries < 1
                || maxQueries > 1000
                || queryWindowHours < 1
                || queryWindowHours > 168)
            throw new IllegalStateException("Invalid online payment deadline/reconciliation configuration");
        for (var entry : gateways.entrySet()) {
            var config = entry.getValue();
            if (!config.isEnabled()) continue;
            if (!Set.of(PaymentProvider.MOMO, PaymentProvider.VNPAY, PaymentProvider.PAYOS)
                            .contains(entry.getKey())
                    || config.getMerchantId().isBlank()
                    || config.getSecretKey().isBlank()
                    || (entry.getKey() != PaymentProvider.VNPAY
                            && config.getAccessKey().isBlank()))
                throw new IllegalStateException("Missing credentials for " + entry.getKey());
            requireHttps(config.getApiUrl());
            requireHttps(config.getCallbackUrl());
            if (entry.getKey() == PaymentProvider.VNPAY) requireHttps(config.getCheckoutUrl());
            URI base = URI.create(returnBaseUrl);
            if (base.getHost() == null
                    || base.getUserInfo() != null
                    || base.getQuery() != null
                    || base.getFragment() != null
                    || !("https".equals(base.getScheme())
                            || ("http".equals(base.getScheme()) && "localhost".equals(base.getHost()))))
                throw new IllegalStateException("Invalid payment return base URL");
        }
    }

    private void requireHttps(String raw) {
        URI uri = URI.create(raw);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalStateException("Enabled payment gateway requires HTTPS URLs");
    }
}
