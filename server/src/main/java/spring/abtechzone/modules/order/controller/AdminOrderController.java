package spring.abtechzone.modules.order.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.order.dto.request.AdminOrderSearchRequest;
import spring.abtechzone.modules.order.dto.request.OrderStatusUpdateRequest;
import spring.abtechzone.modules.order.dto.response.OrderDetailResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.dto.response.OrderSummaryResponse;
import spring.abtechzone.modules.order.service.OrderLifecycleService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Admin Orders",
        description =
                "Admin order management: search/list, detail, and status transitions via the shared service policy")
public class AdminOrderController {

    OrderLifecycleService orderLifecycleService;
    AuthService authService;
    UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Search orders (paginated)",
            description =
                    "Admin order list with optional search (order code, recipient name, phone), status and date-range "
                            + "filters. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Orders retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Page<OrderSummaryResponse>> searchOrders(@Valid @ModelAttribute AdminOrderSearchRequest request) {
        return ApiResult.<Page<OrderSummaryResponse>>builder()
                .result(orderLifecycleService.getAdminOrders(request))
                .build();
    }

    @GetMapping("/{orderCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin order detail", description = "Full order snapshot with items and status history")
    @ApiResponse(responseCode = "200", description = "Order detail returned")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    ApiResult<OrderDetailResponse> getOrderDetail(@PathVariable String orderCode) {
        return ApiResult.<OrderDetailResponse>builder()
                .result(orderLifecycleService.getAdminOrderDetail(orderCode))
                .build();
    }

    @PatchMapping("/{orderCode}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update order status",
            description =
                    "Apply an allowed lifecycle transition (PENDING->CONFIRMED|CANCELLED, CONFIRMED->SHIPPING|CANCELLED, "
                            + "SHIPPING->DELIVERED) through the shared service state machine. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @ApiResponse(responseCode = "409", description = "Transition not allowed")
    ApiResult<OrderResponse> updateStatus(
            @PathVariable String orderCode, @RequestBody @Valid OrderStatusUpdateRequest request) {
        User admin = currentAdmin();
        return ApiResult.<OrderResponse>builder()
                .result(orderLifecycleService.updateOrderStatus(
                        orderCode, request.getStatus(), request.getNote(), admin))
                .build();
    }

    private User currentAdmin() {
        String username = authService.getCurrentUsername();
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new spring.abtechzone.common.exception.AppException(
                        spring.abtechzone.common.exception.ErrorCode.USER_NOT_FOUND));
    }
}
