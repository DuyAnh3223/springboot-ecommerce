package spring.abtechzone.modules.payment.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.constant.PaymentMethod;

@Component
@RequiredArgsConstructor
public class PaymentMockPolicy {

    private final Environment environment;

    @Value("${app.payment.mock-enabled:false}")
    private boolean mockEnabled;

    public boolean isEnabled() {
        var profiles = Arrays.asList(environment.getActiveProfiles());
        return mockEnabled && !profiles.contains("prod") && (profiles.contains("dev") || profiles.contains("test"));
    }

    public void requireAvailable(PaymentMethod method) {
        if (method == PaymentMethod.MOCK && !isEnabled()) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_AVAILABLE);
        }
    }

    public void requireEnabled() {
        if (!isEnabled()) {
            throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_AVAILABLE);
        }
    }
}
