package spring.abtechzone.modules.order.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.request.OrderCancelRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderDetailResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.dto.response.OrderSummaryResponse;
import spring.abtechzone.modules.order.service.OrderService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Orders",
        description =
                "Order lifecycle management: checkout preview, order creation with distributed locking, customer order history and cancellation")
public class OrderController {

    OrderService orderService;
    AuthService authService;
    UserRepository userRepository;

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

    @GetMapping("/me")
    @Operation(
            summary = "Get my orders (paginated)",
            description =
                    "Current user's orders, newest first, with an optional status filter. The user is resolved from the "
                            + "authentication context via AuthService; no user id is accepted from the request")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<Page<OrderSummaryResponse>> getMyOrders(
            @RequestParam(required = false) @Parameter(description = "Optional order status filter") OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.<Page<OrderSummaryResponse>>builder()
                .result(orderService.getMyOrders(status, page, size, currentUser()))
                .build();
    }

    @GetMapping("/{orderCode}")
    @Operation(
            summary = "Get order detail",
            description =
                    "Detail of an order belonging to the current user. A code that does not exist or belongs to another "
                            + "user returns 404 so order existence is not disclosed (owner-safe)")
    @ApiResponse(responseCode = "200", description = "Order detail returned")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "404", description = "Order not found or not owned by the current user")
    ApiResult<OrderDetailResponse> getOrderDetail(@PathVariable String orderCode) {
        return ApiResult.<OrderDetailResponse>builder()
                .result(orderService.getMyOrderDetail(orderCode, currentUser()))
                .build();
    }

    @PostMapping("/{orderCode}/cancel")
    @Operation(
            summary = "Cancel my order",
            description =
                    "Cancel an order owned by the current user while it is still PENDING. Stock and voucher usage are "
                            + "restored exactly once; the transition is guarded by a pessimistic order lock")
    @ApiResponse(responseCode = "200", description = "Order cancelled (or already cancelled, idempotent)")
    @ApiResponse(responseCode = "400", description = "Missing or invalid reason")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    @ApiResponse(responseCode = "404", description = "Order not found or not owned by the current user")
    @ApiResponse(responseCode = "409", description = "Order status does not allow cancellation")
    ApiResult<OrderResponse> cancelOrder(
            @PathVariable String orderCode, @RequestBody @Valid OrderCancelRequest request) {
        return ApiResult.<OrderResponse>builder()
                .result(orderService.cancelOrder(orderCode, request.getReason(), currentUser()))
                .build();
    }

    private User currentUser() {
        String username = authService.getCurrentUsername();
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new spring.abtechzone.common.exception.AppException(
                        spring.abtechzone.common.exception.ErrorCode.USER_NOT_FOUND));
    }
}
