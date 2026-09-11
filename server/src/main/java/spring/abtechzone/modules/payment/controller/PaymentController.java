package spring.abtechzone.modules.payment.controller;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.payment.dto.response.OrderPaymentsResponse;
import spring.abtechzone.modules.payment.service.OrderPaymentService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@RestController
@RequestMapping("/orders/{orderCode}/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {

    OrderPaymentService orderPaymentService;
    AuthService authService;
    UserRepository userRepository;

    @GetMapping
    ApiResult<OrderPaymentsResponse> getMyPayments(@PathVariable String orderCode) {
        return ApiResult.<OrderPaymentsResponse>builder()
                .result(orderPaymentService.getMyPayments(orderCode, currentUser()))
                .build();
    }

    private User currentUser() {
        return userRepository
                .findByUsername(authService.getCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
