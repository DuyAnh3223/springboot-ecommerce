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
public class MomoGateway implements PaymentGateway {
    private final OnlinePaymentProperties properties;
    private final GatewayHttp http;

    public PaymentProvider provider() {
        return PaymentProvider.MOMO;
    }

    public Checkout create(Attempt a) {
        var c = properties.require(provider());
        var p = JSON.createObjectNode();
        p.put("partnerCode", c.getMerchantId());
        p.put("requestId", a.merchantId());
        p.put("orderId", a.merchantId());
        p.put("amount", a.amount().longValueExact());
        p.put("orderInfo", "Thanh toan " + a.merchantId());
        p.put(
                "redirectUrl",
                properties.getReturnBaseUrl().replaceAll("/$", "") + "/profile/orders/" + encode(a.orderCode())
                        + "?paymentReturn=1");
        p.put("ipnUrl", c.getCallbackUrl());
        p.put("extraData", "");
        p.put("requestType", "captureWallet");
        p.put("autoCapture", true);
        p.put("lang", "vi");
        p.put(
                "signature",
                sign(
                        p,
                        "amount",
                        "extraData",
                        "ipnUrl",
                        "orderId",
                        "orderInfo",
                        "partnerCode",
                        "redirectUrl",
                        "requestId",
                        "requestType"));
        JsonNode r = http.exchange(c.getApiUrl() + "/create", Map.of(), p);
        verify(
                sign(r, "amount", "orderId", "partnerCode", "payUrl", "requestId", "responseTime", "resultCode"),
                text(r, "signature"));
        validateCorrelation(r, a, a.merchantId());
        if (!"0".equals(text(r, "resultCode"))) throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
        return new Checkout(text(r, "payUrl"), a.merchantId());
    }

    public Result verifyCallback(JsonNode p) {
        verify(
                sign(
                        p,
                        "amount",
                        "extraData",
                        "message",
                        "orderId",
                        "orderInfo",
                        "orderType",
                        "partnerCode",
                        "payType",
                        "requestId",
                        "responseTime",
                        "resultCode",
                        "transId"),
                text(p, "signature"));
        if (!text(p, "orderId").equals(text(p, "requestId"))) throw new GatewayException(GatewayException.Kind.DATA);
        return result(p);
    }

    public Result query(Attempt a) {
        var c = properties.require(provider());
        var p = JSON.createObjectNode();
        String requestId = UUID.randomUUID().toString();
        p.put("partnerCode", c.getMerchantId());
        p.put("orderId", a.merchantId());
        p.put("requestId", requestId);
        p.put("lang", "vi");
        p.put("signature", sign(p, "orderId", "partnerCode", "requestId"));
        // This API's documented response has no signature; authenticated HTTPS is the response trust boundary.
        JsonNode r = http.exchange(c.getApiUrl() + "/query", Map.of(), p);
        validateCorrelation(r, a, requestId);
        return result(r);
    }

    private void validateCorrelation(JsonNode r, Attempt a, String requestId) {
        if (!text(r, "orderId").equals(a.merchantId())
                || !text(r, "requestId").equals(requestId)
                || !text(r, "partnerCode").equals(properties.require(provider()).getMerchantId())
                || new BigDecimal(text(r, "amount")).compareTo(a.amount()) != 0)
            throw new GatewayException(GatewayException.Kind.DATA);
    }

    private String sign(JsonNode p, String... names) {
        var c = properties.require(provider());
        Map<String, String> values = new TreeMap<>();
        values.put("accessKey", c.getAccessKey());
        for (String name : names) values.put(name, text(p, name));
        return hmac("HmacSHA256", c.getSecretKey(), pairs(values, false));
    }

    private Result result(JsonNode p) {
        if (!text(p, "partnerCode").equals(properties.require(provider()).getMerchantId()))
            throw new GatewayException(GatewayException.Kind.DATA);
        String code = text(p, "resultCode");
        Outcome outcome = Set.of("0", "9000").contains(code)
                ? Outcome.SUCCEEDED
                : Set.of("1001", "1002", "1003", "1004", "1005", "1006", "1007", "1017", "1026", "4001", "4002", "4100")
                                .contains(code)
                        ? Outcome.FAILED
                        : Outcome.PENDING;
        return new Result(
                text(p, "orderId"),
                new BigDecimal(text(p, "amount")),
                "VND",
                outcome,
                outcome == Outcome.SUCCEEDED ? text(p, "transId") : null,
                null);
    }
}
