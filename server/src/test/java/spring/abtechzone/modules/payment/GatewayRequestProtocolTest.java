package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.gateway.*;

class GatewayRequestProtocolTest {
    GatewayHttp http = mock(GatewayHttp.class);
    String back = "http://localhost:3000/profile/orders/ORD1?paymentReturn=1";

    OnlinePaymentProperties properties(PaymentProvider provider) {
        var c = new OnlinePaymentProperties.Gateway();
        c.setEnabled(true);
        c.setMerchantId("merchant");
        c.setAccessKey("access");
        c.setSecretKey("secret");
        c.setApiUrl("https://gateway.test");
        c.setCallbackUrl("https://merchant.test/momo");
        var p = new OnlinePaymentProperties();
        p.setGateways(Map.of(provider, c));
        return p;
    }

    PaymentGateway.Attempt attempt() {
        var date = OffsetDateTime.parse("2026-09-06T08:00:00Z");
        return new PaymentGateway.Attempt(
                "123", new BigDecimal("10000.00"), "VND", "ORD1", date, date.plusMinutes(15), "127.0.0.1");
    }

    String sign(String algorithm, String text) throws Exception {
        var mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), algorithm));
        return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
    }

    ObjectNode json(String text) throws Exception {
        return (ObjectNode) GatewayCrypto.JSON.readTree(text);
    }

    @Test
    void momoCreateSignsExactCaptureWalletContractAndVerifiesResponse() throws Exception {
        var response = json(
                "{\"partnerCode\":\"merchant\",\"requestId\":\"123\",\"orderId\":\"123\",\"amount\":10000,\"responseTime\":1,\"resultCode\":0,\"payUrl\":\"https://test-payment.momo.vn/pay\"}");
        response.put(
                "signature",
                sign(
                        "HmacSHA256",
                        "accessKey=access&amount=10000&orderId=123&partnerCode=merchant&payUrl=https://test-payment.momo.vn/pay&requestId=123&responseTime=1&resultCode=0"));
        when(http.exchange(eq("https://gateway.test/create"), anyMap(), any())).thenAnswer(call -> {
            JsonNode p = call.getArgument(2);
            assertThat(p.path("signature").asText())
                    .isEqualTo(sign(
                            "HmacSHA256",
                            "accessKey=access&amount=10000&extraData=&ipnUrl=https://merchant.test/momo&orderId=123&orderInfo=Thanh toan 123&partnerCode=merchant&redirectUrl="
                                    + back + "&requestId=123&requestType=captureWallet"));
            assertThat(p.path("amount").isIntegralNumber()).isTrue();
            assertThat(p.path("autoCapture").asBoolean()).isTrue();
            return response;
        });
        var gateway = new MomoGateway(properties(PaymentProvider.MOMO), http);
        assertThat(gateway.create(attempt()).url()).isEqualTo("https://test-payment.momo.vn/pay");
        response.put("payUrl", "https://attacker.test/");
        assertThatThrownBy(() -> gateway.create(attempt())).isInstanceOf(GatewayException.class);
    }

    @Test
    void momoQueryUsesFreshSignedRequestAndChecksCorrelation() throws Exception {
        when(http.exchange(eq("https://gateway.test/query"), anyMap(), any())).thenAnswer(call -> {
            JsonNode p = call.getArgument(2);
            assertThat(p.path("signature").asText())
                    .isEqualTo(sign(
                            "HmacSHA256",
                            "accessKey=access&orderId=123&partnerCode=merchant&requestId="
                                    + p.path("requestId").asText()));
            var response = json(
                    "{\"partnerCode\":\"merchant\",\"orderId\":\"123\",\"amount\":10000,\"resultCode\":0,\"transId\":456}");
            response.put("requestId", p.path("requestId").asText());
            return response;
        });
        assertThat(new MomoGateway(properties(PaymentProvider.MOMO), http)
                        .query(attempt())
                        .transactionId())
                .isEqualTo("456");
    }

    @Test
    void payosCreateSignsFiveFieldsAndSendsExpirySeparately() throws Exception {
        var data = json(
                "{\"amount\":10000,\"checkoutUrl\":\"https://pay.payos.vn/link\",\"currency\":\"VND\",\"orderCode\":123,\"paymentLinkId\":\"link\",\"status\":\"PENDING\"}");
        var response = GatewayCrypto.JSON.createObjectNode();
        response.set("data", data);
        response.put(
                "signature",
                sign(
                        "HmacSHA256",
                        "amount=10000&checkoutUrl=https://pay.payos.vn/link&currency=VND&orderCode=123&paymentLinkId=link&status=PENDING"));
        when(http.exchange(eq("https://gateway.test/v2/payment-requests"), anyMap(), any()))
                .thenAnswer(call -> {
                    Map<String, String> headers = call.getArgument(1);
                    JsonNode p = call.getArgument(2);
                    assertThat(headers).containsEntry("x-client-id", "merchant").containsEntry("x-api-key", "access");
                    assertThat(p.path("signature").asText())
                            .isEqualTo(sign(
                                    "HmacSHA256",
                                    "amount=10000&cancelUrl=" + back + "&description=DH 123&orderCode=123&returnUrl="
                                            + back));
                    assertThat(p.path("expiredAt").asLong())
                            .isEqualTo(attempt().deadline().toEpochSecond());
                    return response;
                });
        assertThat(new PayosGateway(properties(PaymentProvider.PAYOS), http)
                        .create(attempt())
                        .providerId())
                .isEqualTo("link");
    }

    @Test
    void payosQueryVerifiesNestedTransactionsAndUsesRealReference() throws Exception {
        var data = json(
                "{\"id\":\"link\",\"orderCode\":123,\"amount\":10000,\"amountPaid\":10000,\"status\":\"PAID\",\"transactions\":[{\"reference\":\"456\",\"amount\":10000}]}");
        var response = GatewayCrypto.JSON.createObjectNode();
        response.set("data", data);
        response.put(
                "signature",
                sign(
                        "HmacSHA256",
                        "amount=10000&amountPaid=10000&id=link&orderCode=123&status=PAID&transactions=[{\"amount\":10000,\"reference\":\"456\"}]"));
        when(http.exchange(eq("https://gateway.test/v2/payment-requests/123"), anyMap(), isNull()))
                .thenReturn(response);
        var gateway = new PayosGateway(properties(PaymentProvider.PAYOS), http);
        assertThat(gateway.query(attempt()).transactionId()).isEqualTo("456");
        data.put("amountPaid", 10001);
        assertThatThrownBy(() -> gateway.query(attempt())).isInstanceOf(GatewayException.class);
    }

    @Test
    void vnpayQueryUsesPipeSignatureAndSignedTransactionStatus() throws Exception {
        var response = json(
                "{\"vnp_ResponseId\":\"reply\",\"vnp_Command\":\"querydr\",\"vnp_ResponseCode\":\"00\",\"vnp_Message\":\"ok\",\"vnp_TmnCode\":\"merchant\",\"vnp_TxnRef\":\"123\",\"vnp_Amount\":\"1000000\",\"vnp_BankCode\":\"NCB\",\"vnp_PayDate\":\"20260906150100\",\"vnp_TransactionNo\":\"456\",\"vnp_TransactionType\":\"01\",\"vnp_TransactionStatus\":\"00\",\"vnp_OrderInfo\":\"Query 123\"}");
        response.put(
                "vnp_SecureHash",
                sign(
                        "HmacSHA512",
                        "reply|querydr|00|ok|merchant|123|1000000|NCB|20260906150100|456|01|00|Query 123||"));
        when(http.exchange(eq("https://gateway.test"), anyMap(), any())).thenAnswer(call -> {
            JsonNode p = call.getArgument(2);
            assertThat(p.path("vnp_SecureHash").asText())
                    .isEqualTo(sign(
                            "HmacSHA512",
                            p.path("vnp_RequestId").asText() + "|2.1.0|querydr|merchant|123|20260906150000|"
                                    + p.path("vnp_CreateDate").asText() + "|127.0.0.1|Query 123"));
            return response;
        });
        assertThat(new VnpayGateway(properties(PaymentProvider.VNPAY), http)
                        .query(attempt())
                        .outcome())
                .isEqualTo(PaymentGateway.Outcome.SUCCEEDED);
    }
}
