package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.gateway.*;

class ProviderCallbackProtocolTest {
    private OnlinePaymentProperties properties(PaymentProvider provider) {
        var c = new OnlinePaymentProperties.Gateway();
        c.setEnabled(true);
        c.setMerchantId("merchant");
        c.setAccessKey("access");
        c.setSecretKey("secret");
        var p = new OnlinePaymentProperties();
        p.setGateways(Map.of(provider, c));
        return p;
    }
    // Fixtures sign an independently specified canonical string, never the production canonicalizer.
    private String signature(String algorithm, String data) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8), algorithm));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private ObjectNode momo(String code) throws Exception {
        var data = GatewayCrypto.JSON.createObjectNode();
        data.put("amount", 10000);
        data.put("extraData", "");
        data.put("message", "ok");
        data.put("orderId", "123");
        data.put("orderInfo", "Order 123");
        data.put("orderType", "momo_wallet");
        data.put("partnerCode", "merchant");
        data.put("payType", "qr");
        data.put("requestId", "123");
        data.put("responseTime", 1788680000000L);
        data.put("resultCode", code);
        data.put("transId", 98765);
        data.put(
                "signature",
                signature(
                        "HmacSHA256",
                        "accessKey=access&amount=10000&extraData=&message=ok&orderId=123&orderInfo=Order 123&orderType=momo_wallet&partnerCode=merchant&payType=qr&requestId=123&responseTime=1788680000000&resultCode="
                                + code + "&transId=98765"));
        return data;
    }

    @Test
    void momoSignedSuccessHasTransactionIdentity() throws Exception {
        var result =
                new MomoGateway(properties(PaymentProvider.MOMO), mock(GatewayHttp.class)).verifyCallback(momo("0"));
        assertThat(result.outcome()).isEqualTo(PaymentGateway.Outcome.SUCCEEDED);
        assertThat(result.transactionId()).isEqualTo("98765");
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
    }

    @Test
    void momoPendingAndDeclineHaveDifferentMeaning() throws Exception {
        var gateway = new MomoGateway(properties(PaymentProvider.MOMO), mock(GatewayHttp.class));
        assertThat(gateway.verifyCallback(momo("7002")).outcome()).isEqualTo(PaymentGateway.Outcome.PENDING);
        assertThat(gateway.verifyCallback(momo("1006")).outcome()).isEqualTo(PaymentGateway.Outcome.FAILED);
        assertThat(gateway.verifyCallback(momo("9000")).outcome()).isEqualTo(PaymentGateway.Outcome.SUCCEEDED);
    }

    @Test
    void momoTamperedMoneyFailsSignature() throws Exception {
        var data = momo("0");
        data.put("amount", 1);
        assertThatThrownBy(() ->
                        new MomoGateway(properties(PaymentProvider.MOMO), mock(GatewayHttp.class)).verifyCallback(data))
                .isInstanceOf(GatewayException.class);
    }

    @Test
    void momoWrongMerchantEvenWithValidSignatureIsRejected() throws Exception {
        var props = properties(PaymentProvider.MOMO);
        props.getGateways().get(PaymentProvider.MOMO).setMerchantId("another");
        var payload = momo("0");
        assertThatThrownBy(() -> new MomoGateway(props, mock(GatewayHttp.class)).verifyCallback(payload))
                .isInstanceOf(GatewayException.class);
    }

    private ObjectNode vnpay(String status) throws Exception {
        var data = GatewayCrypto.JSON.createObjectNode();
        data.put("vnp_Amount", "1000000");
        data.put("vnp_ResponseCode", "00");
        data.put("vnp_TmnCode", "merchant");
        data.put("vnp_TransactionNo", "456");
        data.put("vnp_TransactionStatus", status);
        data.put("vnp_TxnRef", "123");
        data.put(
                "vnp_SecureHash",
                signature(
                        "HmacSHA512",
                        "vnp_Amount=1000000&vnp_ResponseCode=00&vnp_TmnCode=merchant&vnp_TransactionNo=456&vnp_TransactionStatus="
                                + status + "&vnp_TxnRef=123"));
        return data;
    }

    @Test
    void vnpayRequiresBothSuccessCodesAndConvertsMoney() throws Exception {
        var gateway = new VnpayGateway(properties(PaymentProvider.VNPAY), mock(GatewayHttp.class));
        assertThat(gateway.verifyCallback(vnpay("00")).amount()).isEqualByComparingTo("10000");
        assertThat(gateway.verifyCallback(vnpay("01")).outcome()).isEqualTo(PaymentGateway.Outcome.PENDING);
        assertThat(gateway.verifyCallback(vnpay("02")).outcome()).isEqualTo(PaymentGateway.Outcome.FAILED);
    }

    @Test
    void vnpayTamperedReferenceIsRejected() throws Exception {
        var data = vnpay("00");
        data.put("vnp_TxnRef", "another");
        assertThatThrownBy(() -> new VnpayGateway(properties(PaymentProvider.VNPAY), mock(GatewayHttp.class))
                        .verifyCallback(data))
                .isInstanceOf(GatewayException.class);
    }

    private ObjectNode payos() throws Exception {
        var data = GatewayCrypto.JSON.createObjectNode();
        data.put("amount", 10000);
        data.put("code", "00");
        data.put("currency", "VND");
        data.put("orderCode", 123);
        data.put("paymentLinkId", "link123");
        data.put("reference", "456");
        var wrapper = GatewayCrypto.JSON.createObjectNode();
        wrapper.set("data", data);
        wrapper.put(
                "signature",
                signature(
                        "HmacSHA256",
                        "amount=10000&code=00&currency=VND&orderCode=123&paymentLinkId=link123&reference=456"));
        return wrapper;
    }

    @Test
    void payosUsesSignedInnerDataAndRealReference() throws Exception {
        var payload = payos();
        payload.put("success", false);
        payload.put("code", "99");
        var result =
                new PayosGateway(properties(PaymentProvider.PAYOS), mock(GatewayHttp.class)).verifyCallback(payload);
        assertThat(result.outcome()).isEqualTo(PaymentGateway.Outcome.SUCCEEDED);
        assertThat(result.transactionId()).isEqualTo("456");
        assertThat(result.providerId()).isEqualTo("link123");
    }

    @Test
    void payosTamperedSignedDataIsRejected() throws Exception {
        var payload = payos();
        ((ObjectNode) payload.get("data")).put("amount", 1);
        assertThatThrownBy(() -> new PayosGateway(properties(PaymentProvider.PAYOS), mock(GatewayHttp.class))
                        .verifyCallback(payload))
                .isInstanceOf(GatewayException.class);
    }
}
