package spring.abtechzone.modules.payment.gateway;

import static spring.abtechzone.modules.payment.gateway.GatewayCrypto.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;

@Component
@RequiredArgsConstructor
public class VnpayGateway implements PaymentGateway {
    private final OnlinePaymentProperties properties;
    private final GatewayHttp http;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public PaymentProvider provider() {
        return PaymentProvider.VNPAY;
    }

    public Checkout create(Attempt a) {
        var c = properties.require(provider());
        Map<String, String> p = new TreeMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", c.getMerchantId());
        p.put(
                "vnp_Amount",
                a.amount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", a.merchantId());
        p.put("vnp_OrderInfo", "Thanh toan " + a.merchantId());
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_IpAddr", a.clientIp());
        p.put("vnp_CreateDate", DATE.format(a.createdAt()));
        p.put("vnp_ExpireDate", DATE.format(a.deadline()));
        p.put(
                "vnp_ReturnUrl",
                properties.getReturnBaseUrl().replaceAll("/$", "") + "/profile/orders/" + encode(a.orderCode())
                        + "?paymentReturn=1");
        String data = pairs(p, true);
        return new Checkout(
                c.getCheckoutUrl() + "?" + data + "&vnp_SecureHash=" + hmac("HmacSHA512", c.getSecretKey(), data),
                a.merchantId());
    }

    public Result verifyCallback(JsonNode payload) {
        var c = properties.require(provider());
        Map<String, String> p = strings(payload);
        String signature = p.remove("vnp_SecureHash");
        p.remove("vnp_SecureHashType");
        p.entrySet()
                .removeIf(e -> !e.getKey().startsWith("vnp_") || e.getValue().isEmpty());
        verify(hmac("HmacSHA512", c.getSecretKey(), pairs(p, true)), signature);
        return result(payload);
    }

    public Result query(Attempt a) {
        var c = properties.require(provider());
        var p = JSON.createObjectNode();
        p.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "querydr");
        p.put("vnp_TmnCode", c.getMerchantId());
        p.put("vnp_TxnRef", a.merchantId());
        p.put("vnp_TransactionDate", DATE.format(a.createdAt()));
        p.put("vnp_CreateDate", DATE.format(Instant.now()));
        p.put("vnp_IpAddr", a.clientIp());
        p.put("vnp_OrderInfo", "Query " + a.merchantId());
        p.put(
                "vnp_SecureHash",
                hmac(
                        "HmacSHA512",
                        c.getSecretKey(),
                        fields(
                                p,
                                "|",
                                "vnp_RequestId",
                                "vnp_Version",
                                "vnp_Command",
                                "vnp_TmnCode",
                                "vnp_TxnRef",
                                "vnp_TransactionDate",
                                "vnp_CreateDate",
                                "vnp_IpAddr",
                                "vnp_OrderInfo")));
        JsonNode r = http.exchange(c.getApiUrl(), Map.of(), p);
        verify(
                hmac(
                        "HmacSHA512",
                        c.getSecretKey(),
                        fields(
                                r,
                                "|",
                                "vnp_ResponseId",
                                "vnp_Command",
                                "vnp_ResponseCode",
                                "vnp_Message",
                                "vnp_TmnCode",
                                "vnp_TxnRef",
                                "vnp_Amount",
                                "vnp_BankCode",
                                "vnp_PayDate",
                                "vnp_TransactionNo",
                                "vnp_TransactionType",
                                "vnp_TransactionStatus",
                                "vnp_OrderInfo",
                                "vnp_PromotionCode",
                                "vnp_PromotionAmount")),
                text(r, "vnp_SecureHash"));
        if (!a.merchantId().equals(text(r, "vnp_TxnRef"))) throw new GatewayException(GatewayException.Kind.DATA);
        if (!"00".equals(text(r, "vnp_ResponseCode"))) throw new GatewayException(GatewayException.Kind.UNAVAILABLE);
        return result(r);
    }

    private Result result(JsonNode p) {
        if (!properties.require(provider()).getMerchantId().equals(text(p, "vnp_TmnCode")))
            throw new GatewayException(GatewayException.Kind.DATA);
        String response = text(p, "vnp_ResponseCode"), status = text(p, "vnp_TransactionStatus");
        Outcome outcome = "00".equals(response) && "00".equals(status)
                ? Outcome.SUCCEEDED
                : Set.of("02", "03").contains(status) ? Outcome.FAILED : Outcome.PENDING;
        return new Result(
                text(p, "vnp_TxnRef"),
                new BigDecimal(text(p, "vnp_Amount")).movePointLeft(2),
                "VND",
                outcome,
                outcome == Outcome.SUCCEEDED ? text(p, "vnp_TransactionNo") : null,
                null);
    }
}
