package spring.abtechzone.modules.payment.gateway;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.*;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;

@Service
@RequiredArgsConstructor
public class GatewayRegistry {
    private final List<PaymentGateway> gateways;
    private final OnlinePaymentProperties properties;

    public PaymentGateway require(PaymentProvider provider) {
        properties.require(provider);
        return gateways.stream()
                .filter(g -> g.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_AVAILABLE));
    }

    public void validateMoney(PaymentProvider provider, BigDecimal amount, String currency) {
        require(provider);
        try {
            long money = amount.longValueExact();
            long min = provider == PaymentProvider.MOMO ? 1000 : provider == PaymentProvider.VNPAY ? 5000 : 1;
            long max = provider == PaymentProvider.MOMO ? 50_000_000L : 999_999_999L;
            if (!"VND".equals(currency) || money < min || money > max) throw new ArithmeticException();
        } catch (ArithmeticException | NullPointerException e) {
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }
}
