package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import spring.abtechzone.modules.payment.controller.PaymentMockController;

class PaymentMockConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withUserConfiguration(PaymentMockController.class);

    @Test
    void controllerIsNotRegisteredWhenFlagIsDisabled() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test", "app.payment.mock-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PaymentMockController.class));
    }

    @Test
    void controllerIsNotRegisteredInProductionEvenWhenFlagIsEnabled() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test,prod", "app.payment.mock-enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(PaymentMockController.class));
    }
}
