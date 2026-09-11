package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;

class OnlinePaymentConfigurationTest {
    @Configuration
    @EnableConfigurationProperties(OnlinePaymentProperties.class)
    static class Config {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @Test
    void noCredentialsMeansNoOnlineProviders() {
        runner.run(context -> assertThat(
                        context.getBean(OnlinePaymentProperties.class).enabledProviders())
                .isEmpty());
    }

    @Test
    void enabledProviderWithoutCredentialsFailsStartup() {
        runner.withPropertyValues("app.payment.online.gateways.MOMO.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void onlyConfiguredVnpayIsEnabled() {
        runner.withPropertyValues(
                        "app.payment.online.gateways.VNPAY.enabled=true",
                        "app.payment.online.gateways.VNPAY.merchant-id=merchant",
                        "app.payment.online.gateways.VNPAY.secret-key=secret",
                        "app.payment.online.gateways.VNPAY.api-url=https://sandbox.vnpayment.vn/query",
                        "app.payment.online.gateways.VNPAY.checkout-url=https://sandbox.vnpayment.vn/pay",
                        "app.payment.online.gateways.VNPAY.callback-url=https://merchant.test/payments/callbacks/vnpay")
                .run(context -> assertThat(
                                context.getBean(OnlinePaymentProperties.class).enabledProviders())
                        .containsExactly(PaymentProvider.VNPAY));
    }
}
