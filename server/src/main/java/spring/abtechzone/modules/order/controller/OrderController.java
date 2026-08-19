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
                    "Read-only pre-order summary for the selected cart items (selectedSkuIds). Returns a reviewed snapshot "
                            + "with server-authoritative amounts, typed sellability/voucher issues and canPlaceOrder. "
                            + "Does NOT create an order or modify any state")
    @ApiResponse(
            responseCode = "200",
            description = "Checkout review returned (business issues are typed in the response)")
    @ApiResponse(responseCode = "400", description = "Invalid selection, or selected SKU not in the active cart")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<CheckoutResponse> checkoutReview(@RequestBody @Valid CheckoutRequest request) {
        return ApiResult.<CheckoutResponse>builder()
                .result(orderService.checkoutReview(request))
                .build();
    }

    @PostMapping
    @Operation(
            summary = "Create order",
            description =
                    "Place a COD order from a reviewed checkout snapshot (see POST /orders/checkout-review). "
                            + "Idempotency-Key must be a UUID; the same key with the same request replays the original order, "
                            + "the same key with a different request returns 409. Distributed locks (Redisson) are acquired "
                            + "before the transaction; stock/voucher/cart/order mutations share one rollback boundary. "
                            + "If any order-affecting state changed since review, 409 CHECKOUT_CHANGED returns the latest review")
    @ApiResponse(responseCode = "200", description = "Order created successfully")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid Idempotency-Key, invalid reviewed checkout, address XOR violation, or COD only")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(
            responseCode = "409",
            description = "IDEMPOTENCY_KEY_REUSED or CHECKOUT_CHANGED (latest review in result)")
    @ApiResponse(responseCode = "503", description = "System busy — lock could not be acquired (retry after a moment)")
    ApiResult<OrderResponse> createOrder(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid CreateOrderRequest request) {
        return ApiResult.<OrderResponse>builder()
                .result(orderService.createOrder(request, idempotencyKey))
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
