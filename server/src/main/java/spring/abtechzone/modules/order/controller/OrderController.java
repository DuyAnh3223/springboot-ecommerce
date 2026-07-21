package spring.abtechzone.modules.order.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Orders",
        description =
                "Order lifecycle management: checkout preview, order creation with distributed locking, and order history")
public class OrderController {

    OrderService orderService;

    @PostMapping("/checkout-review")
    @Operation(
            summary = "Checkout review",
            description =
                    "Read-only pre-order summary: validates cart items (stock, availability), applies voucher discount, and returns the final price breakdown. Does NOT create an order or modify any state")
    @ApiResponse(responseCode = "200", description = "Checkout summary returned")
    @ApiResponse(responseCode = "400", description = "Cart is empty, insufficient stock, or invalid voucher")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<CheckoutResponse> checkoutReview(@RequestBody @Valid CheckoutRequest request) {
        return ApiResult.<CheckoutResponse>builder()
                .result(orderService.checkoutReview(request))
                .build();
    }

    @PostMapping
    @Operation(
            summary = "Create order",
            description = "Place an order from the authenticated user's active cart. "
                    + "Uses distributed locking (Redisson) to prevent race conditions: "
                    + "user-level deduplication, SKU stock protection, and voucher oversell prevention. "
                    + "Stock is atomically reserved on success")
    @ApiResponse(responseCode = "200", description = "Order created successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Cart empty, insufficient stock, invalid voucher, or address required")
    @ApiResponse(responseCode = "400", description = "System busy — lock could not be acquired (retry after a moment)")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResult.<OrderResponse>builder()
                .result(orderService.createOrder(request))
                .build();
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get orders by user ID",
            description = "Retrieve all orders for a specific user, sorted by creation date descending")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<List<OrderResponse>> getOrdersByUserId(@PathVariable @Parameter(description = "User UUID") UUID userId) {
        return ApiResult.<List<OrderResponse>>builder()
                .result(orderService.getOrdersByUserId(userId))
                .build();
    }
}
