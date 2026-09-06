package spring.abtechzone.modules.payment.controller;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.payment.dto.request.MockPaymentResultRequest;
import spring.abtechzone.modules.payment.dto.response.PaymentCommandResponse;
import spring.abtechzone.modules.payment.service.OrderPaymentService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@RestController
@Profile({"dev & !prod", "test & !prod"})
@ConditionalOnProperty(prefix = "app.payment", name = "mock-enabled", havingValue = "true")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
/** Legacy L1 fixture; not packaged in the application. */
public class PaymentMockController {

    OrderPaymentService orderPaymentService;
    AuthService authService;
    UserRepository userRepository;

    @PostMapping("/orders/{orderCode}/payments/mock-attempts")
    ApiResult<PaymentCommandResponse> retryMock(
            @PathVariable String orderCode, @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResult.<PaymentCommandResponse>builder()
                .result(orderPaymentService.retryMock(orderCode, idempotencyKey, currentUser()))
                .build();
    }

    @PostMapping("/payments/{paymentId}/mock-result")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResult<PaymentCommandResponse> applyMockResult(
            @PathVariable Long paymentId, @RequestBody @Valid MockPaymentResultRequest request) {
        return ApiResult.<PaymentCommandResponse>builder()
                .result(orderPaymentService.applyMockResult(paymentId, request, currentUser()))
                .build();
    }

    private User currentUser() {
        return userRepository
                .findByUsername(authService.getCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
