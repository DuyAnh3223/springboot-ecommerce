package spring.abtechzone.modules.order.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {

    OrderService orderService;

    @PostMapping("/checkout-review")
    ApiResponse<CheckoutResponse> checkoutReview(@RequestBody @Valid CheckoutRequest request) {
        return ApiResponse.<CheckoutResponse>builder()
                .result(orderService.checkoutReview(request))
                .build();
    }

    @PostMapping
    ApiResponse<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.createOrder(request))
                .build();
    }
}
