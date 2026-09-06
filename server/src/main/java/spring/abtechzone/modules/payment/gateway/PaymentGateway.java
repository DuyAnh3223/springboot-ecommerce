package spring.abtechzone.modules.payment.gateway;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import spring.abtechzone.modules.payment.constant.PaymentProvider;

public interface PaymentGateway {
    enum Outcome {
        PENDING,
        SUCCEEDED,
        FAILED
    }

    record Attempt(
            String merchantId,
            BigDecimal amount,
            String currency,
            String orderCode,
            OffsetDateTime createdAt,
            OffsetDateTime deadline,
            String clientIp) {}

    record Checkout(String url, String providerId) {}

    record Result(
            String merchantId,
            BigDecimal amount,
            String currency,
            Outcome outcome,
            String transactionId,
            String providerId) {}

    PaymentProvider provider();

    Checkout create(Attempt attempt);

    Result query(Attempt attempt);

    Result verifyCallback(JsonNode payload);
}
