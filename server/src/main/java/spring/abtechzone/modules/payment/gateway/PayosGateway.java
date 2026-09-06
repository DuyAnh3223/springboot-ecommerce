package spring.abtechzone.modules.payment.gateway;

import static spring.abtechzone.modules.payment.gateway.GatewayCrypto.*;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;

@Component
@RequiredArgsConstructor
public class PayosGateway implements PaymentGateway {
    private final OnlinePaymentProperties properties;
    private final GatewayHttp http;

    public PaymentProvider provider() {
        return PaymentProvider.PAYOS;
    }

    public Checkout create(Attempt a) {
        var c = properties.require(provider());
        var p = JSON.createObjectNode();
        p.put("orderCode", Long.parseLong(a.merchantId()));
        p.put("amount", a.amount().longValueExact());
        p.put("description", "DH " + a.merchantId());
        String returnUrl = properties.getReturnBaseUrl().replaceAll("/$", "") + "/profile/orders/"
                + encode(a.orderCode()) + "?paymentReturn=1";
        p.put("returnUrl", returnUrl);
        p.put("cancelUrl", returnUrl);
        p.put("signature", hmac("HmacSHA256", c.getSecretKey(), payosCanonical(p)));
        p.put("expiredAt", a.deadline().toEpochSecond());
        JsonNode data = verified(http.exchange(c.getApiUrl() + "/v2/payment-requests", headers(), p));
        if (!text(data, "orderCode").equals(a.merchantId())
                || new BigDecimal(text(data, "amount")).compareTo(a.amount()) != 0
                || !"VND".equals(text(data, "currency"))) throw new GatewayException(GatewayException.Kind.DATA);
        return new Checkout(text(data, "checkoutUrl"), text(data, "paymentLinkId"));
    }

    public Result verifyCallback(JsonNode p) {
        JsonNode data = verified(p);
        // Only the signed inner code is authoritative; outer success/code are not signed.
        Outcome outcome = "00".equals(text(data, "code")) ? Outcome.SUCCEEDED : Outcome.PENDING;
        return new Result(
                text(data, "orderCode"),
                new BigDecimal(text(data, "amount")),
                text(data, "currency"),
                outcome,
                outcome == Outcome.SUCCEEDED ? text(data, "reference") : null,
                text(data, "paymentLinkId"));
    }

    public Result query(Attempt a) {
        var c = properties.require(provider());
        JsonNode data =
                verified(http.exchange(c.getApiUrl() + "/v2/payment-requests/" + a.merchantId(), headers(), null));
        if (!text(data, "orderCode").equals(a.merchantId())
                || new BigDecimal(text(data, "amount")).compareTo(a.amount()) != 0)
            throw new GatewayException(GatewayException.Kind.DATA);
        String status = text(data, "status");
        Outcome outcome = "PAID".equals(status)
                ? Outcome.SUCCEEDED
                : Set.of("CANCELLED", "EXPIRED").contains(status)
                                && new BigDecimal(text(data, "amountPaid")).signum() == 0
                        ? Outcome.FAILED
                        : Outcome.PENDING;
        if (new BigDecimal(text(data, "amountPaid")).signum() > 0 && outcome != Outcome.SUCCEEDED)
            throw new GatewayException(GatewayException.Kind.DATA);
        String reference = null;
        BigDecimal amount = a.amount();
        if (outcome == Outcome.SUCCEEDED) {
            JsonNode transactions = data.path("transactions");
            // Multiple/partial transfers need operator review, not a fabricated single transaction reference.
            if (!transactions.isArray() || transactions.size() != 1)
                throw new GatewayException(GatewayException.Kind.DATA);
            reference = text(transactions.get(0), "reference");
            amount = new BigDecimal(text(data, "amountPaid"));
        }
        return new Result(a.merchantId(), amount, "VND", outcome, reference, text(data, "id"));
    }

    private JsonNode verified(JsonNode response) {
        JsonNode data = response.get("data");
        if (data == null || !data.isObject()) throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
        verify(
                hmac("HmacSHA256", properties.require(provider()).getSecretKey(), payosCanonical(data)),
                text(response, "signature"));
        return data;
    }

    private Map<String, String> headers() {
        var c = properties.require(provider());
        return Map.of("x-client-id", c.getMerchantId(), "x-api-key", c.getAccessKey());
    }
}
