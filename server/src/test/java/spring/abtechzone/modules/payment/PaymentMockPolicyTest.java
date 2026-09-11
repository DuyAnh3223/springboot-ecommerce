package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import spring.abtechzone.modules.payment.config.PaymentMockPolicy;

class PaymentMockPolicyTest {

    @Test
    void enabledOnlyForDevOrTestWithoutProd() {
        assertThat(policy(true, "test").isEnabled()).isTrue();
        assertThat(policy(true, "dev").isEnabled()).isTrue();
        assertThat(policy(true, "prod").isEnabled()).isFalse();
        assertThat(policy(true, "dev", "prod").isEnabled()).isFalse();
        assertThat(policy(false, "test").isEnabled()).isFalse();
    }

    private PaymentMockPolicy policy(boolean enabled, String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        PaymentMockPolicy policy = new PaymentMockPolicy(environment);
        ReflectionTestUtils.setField(policy, "mockEnabled", enabled);
        return policy;
    }
}
