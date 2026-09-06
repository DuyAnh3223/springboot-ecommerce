package spring.abtechzone.modules.payment.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.payment.dto.response.OrderPaymentsResponse;
import spring.abtechzone.modules.payment.service.OrderPaymentService;

@RestController
@RequestMapping("/admin/orders/{orderCode}/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminPaymentController {

    OrderPaymentService orderPaymentService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResult<OrderPaymentsResponse> getPayments(@PathVariable String orderCode) {
        return ApiResult.<OrderPaymentsResponse>builder()
                .result(orderPaymentService.getAdminPayments(orderCode))
                .build();
    }
}
