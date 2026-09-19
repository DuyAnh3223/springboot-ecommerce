package spring.abtechzone.modules.payment.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.common.exception.*;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.service.OnlinePaymentService;
import spring.abtechzone.modules.user.repository.UserRepository;

@RestController
@RequiredArgsConstructor
public class PaymentCheckoutController {
    public record Request(@NotNull PaymentProvider provider) {}

    private final OnlinePaymentService payments;
    private final OnlinePaymentProperties properties;
    private final AuthService auth;
    private final UserRepository users;

    @GetMapping("/payments/providers")
    public ApiResult<List<PaymentProvider>> providers() {
        return ApiResult.<List<PaymentProvider>>builder()
                .result(properties.enabledProviders())
                .build();
    }

    @PostMapping("/orders/{orderCode}/payments/checkout")
    public ApiResult<OnlinePaymentService.CheckoutView> checkout(
            @PathVariable String orderCode,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody Request request,
            HttpServletRequest http) {
        var user = users.findByUsername(auth.getCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return ApiResult.<OnlinePaymentService.CheckoutView>builder()
                .result(payments.checkout(orderCode, key, request.provider(), user.getId(), http.getRemoteAddr()))
                .build();
    }
}
