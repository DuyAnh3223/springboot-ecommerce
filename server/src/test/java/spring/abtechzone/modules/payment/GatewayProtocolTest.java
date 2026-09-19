package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.gateway.*;

class GatewayProtocolTest {
    @Test
    void hmacMatchesIndependentRfc4231Vector() {
        assertThat(GatewayCrypto.hmac("HmacSHA256", "Jefe", "what do ya want for nothing?"))
                .isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
        assertThatThrownBy(() -> GatewayCrypto.verify("abcd", "abce")).isInstanceOf(GatewayException.class);
    }

    @Test
    void vnpayCheckoutUsesMinorUnitsAndServerReturn() {
        var properties = new OnlinePaymentProperties();
        var config = new OnlinePaymentProperties.Gateway();
        config.setEnabled(true);
        config.setMerchantId("MERCHANT");
        config.setSecretKey("secret");
        config.setCheckoutUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        properties.setGateways(Map.of(PaymentProvider.VNPAY, config));
        var adapter = new VnpayGateway(properties, mock(GatewayHttp.class));
        var time = OffsetDateTime.parse("2026-09-06T08:00:00Z");
        var result = adapter.create(new PaymentGateway.Attempt(
                "123", new BigDecimal("10000"), "VND", "ORD123", time, time.plusMinutes(15), "127.0.0.1"));
        assertThat(result.url())
                .contains(
                        "vnp_Amount=1000000",
                        "vnp_CreateDate=20260906150000",
                        "vnp_ExpireDate=20260906151500",
                        "vnp_TxnRef=123");
        verifyNoInteractions(mock(GatewayHttp.class));
    }

    @Test
    void payosSortsNestedTransactionKeysAndNormalizesNulls() throws Exception {
        var data = GatewayCrypto.JSON.readTree("{\"z\":null,\"a\":[{\"z\":2,\"a\":1}]}");
        assertThat(GatewayCrypto.payosCanonical(data)).isEqualTo("a=[{\"a\":1,\"z\":2}]&z=");
    }

    @Test
    void duplicateJsonKeysAreRejected() {
        assertThatThrownBy(() -> GatewayCrypto.JSON.readTree("{\"amount\":1,\"amount\":2}"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }
}
