package spring.abtechzone.modules.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentStatus;
import spring.abtechzone.modules.order.dto.request.AdminOrderSearchRequest;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderItem;
import spring.abtechzone.modules.order.entity.OrderStatusHistory;
import spring.abtechzone.modules.order.mapper.OrderMapper;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.order.service.OrderLifecycleService;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRedemptionRepository;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    VoucherRedemptionRepository voucherRedemptionRepository;

    @Mock
    InventoryService inventoryService;

    @Spy
    OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @InjectMocks
    OrderLifecycleService orderService;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private User user;
    private User admin;
    private Order order;
    private OrderItem orderItem;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        user = User.builder().id(userId).username("customer").isActive(true).build();
        admin = User.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .username("admin")
                .isActive(true)
                .build();

        sku = ProductSku.builder()
                .id(100L)
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(1000000.00))
                .build();

        orderItem = OrderItem.builder()
                .id(1L)
                .order(null)
                .sku(sku)
                .skuId(100L)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(1000000.00))
                .build();

        order = Order.builder()
                .id(999L)
                .orderCode("ORD-20260818-ABCD1234")
                .userId(userId)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .subtotalAmount(BigDecimal.valueOf(2000000))
                .shippingFee(BigDecimal.valueOf(30000))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(2030000))
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
        orderItem.setOrder(order);
    }

    private void stubLockedOrder() {
        when(orderRepository.findByOrderCodeForUpdate("ORD-20260818-ABCD1234")).thenReturn(Optional.of(order));
    }

    @Nested
    @DisplayName("cancelOrder — customer owner")
    class CancelOrderTests {

        @Test
        @DisplayName("PENDING order cancel restores stock once, reverses voucher, writes one history")
        void cancelPending_compensatesAndTransitions() {
            stubLockedOrder();
            Voucher voucher =
                    Voucher.builder().id(77L).code("SAVE100K").usedCount(1).build();
            order.setVoucher(voucher);
            order.setVoucherCode("SAVE100K");
            when(voucherRedemptionRepository.reverseRedemptionByOrderId(999L)).thenReturn(1);
            when(voucherRepository.decreaseUsedCount(77L)).thenReturn(1);

            OrderResponse response =
                    orderService.cancelOrder("ORD-20260818-ABCD1234", "  Tôi muốn thay đổi sản phẩm  ", user);

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
            verify(inventoryService).increaseStock(100L, 2, order, sku);
            verify(voucherRedemptionRepository).reverseRedemptionByOrderId(999L);
            verify(voucherRepository).decreaseUsedCount(77L);

            ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
            verify(orderStatusHistoryRepository).save(historyCaptor.capture());
            OrderStatusHistory history = historyCaptor.getValue();
            assertThat(history.getFromStatus()).isEqualTo("PENDING");
            assertThat(history.getToStatus()).isEqualTo("CANCELLED");
            assertThat(history.getActorType()).isEqualTo("CUSTOMER");
            assertThat(history.getActorId()).isEqualTo(userId.toString());
            assertThat(history.getNote()).isEqualTo("Tôi muốn thay đổi sản phẩm");
        }

        @Test
        @DisplayName("Cancel without voucher does not decrement usedCount")
        void cancelPending_withoutVoucher() {
            stubLockedOrder();

            orderService.cancelOrder("ORD-20260818-ABCD1234", "reason", user);

            verify(voucherRedemptionRepository, never()).reverseRedemptionByOrderId(any());
            verify(voucherRepository, never()).decreaseUsedCount(any());
        }

        @Test
        @DisplayName("Voucher-bearing order without active redemption fails before status transition")
        void cancelWithMissingRedemption_failsClosed() {
            stubLockedOrder();
            order.setVoucher(
                    Voucher.builder().id(77L).code("SAVE100K").usedCount(1).build());
            when(voucherRedemptionRepository.reverseRedemptionByOrderId(999L)).thenReturn(0);

            assertThatThrownBy(() -> orderService.cancelOrder("ORD-20260818-ABCD1234", "reason", user))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.SYSTEM_ERROR.getMessage());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(voucherRepository, never()).decreaseUsedCount(any());
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Voucher used-count decrement failure aborts cancellation")
        void cancelWithFailedUsedCountDecrement_failsClosed() {
            stubLockedOrder();
            order.setVoucher(
                    Voucher.builder().id(77L).code("SAVE100K").usedCount(1).build());
            when(voucherRedemptionRepository.reverseRedemptionByOrderId(999L)).thenReturn(1);
            when(voucherRepository.decreaseUsedCount(77L)).thenReturn(0);

            assertThatThrownBy(() -> orderService.cancelOrder("ORD-20260818-ABCD1234", "reason", user))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.SYSTEM_ERROR.getMessage());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Repeated cancel returns current order without second compensation")
        void repeatedCancel_isIdempotent() {
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.CANCELLED);
            stubLockedOrder();

            OrderResponse response = orderService.cancelOrder("ORD-20260818-ABCD1234", "again", user);

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
            verify(inventoryService, never()).increaseStock(any(), anyInt(), any(), any());
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Non-owner cancel returns 404 without disclosure")
        void cancelNonOwner_returns404() {
            stubLockedOrder();

            assertThatThrownBy(() -> orderService.cancelOrder(
                            "ORD-20260818-ABCD1234",
                            "reason",
                            User.builder().id(otherUserId).build()))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cancel from CONFIRMED by customer returns 409 and no mutation")
        void cancelConfirmedByCustomer_returns409() {
            order.setStatus(OrderStatus.CONFIRMED);
            stubLockedOrder();

            assertThatThrownBy(() -> orderService.cancelOrder("ORD-20260818-ABCD1234", "reason", user))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ORDER_STATUS_CONFLICT.getMessage());
            verify(inventoryService, never()).increaseStock(any(), anyInt(), any(), any());
            verify(orderStatusHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateOrderStatus — admin transitions")
    class AdminTransitionTests {

        @Test
        @DisplayName("PENDING -> CONFIRMED writes one history entry")
        void pendingToConfirmed() {
            stubLockedOrder();

            OrderResponse response = orderService.updateOrderStatus(
                    "ORD-20260818-ABCD1234", OrderStatus.CONFIRMED, "Đã xác nhận", admin);

            assertThat(response.getStatus()).isEqualTo("CONFIRMED");
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        }

        @Test
        @DisplayName("Same-target CONFIRMED request is idempotent: no new history")
        void sameTarget_isIdempotent() {
            order.setStatus(OrderStatus.CONFIRMED);
            stubLockedOrder();

            OrderResponse response =
                    orderService.updateOrderStatus("ORD-20260818-ABCD1234", OrderStatus.CONFIRMED, "again", admin);

            assertThat(response.getStatus()).isEqualTo("CONFIRMED");
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("SHIPPING -> DELIVERED sets payment PAID")
        void delivered_setsPaymentPaid() {
            order.setStatus(OrderStatus.SHIPPING);
            stubLockedOrder();

            OrderResponse response =
                    orderService.updateOrderStatus("ORD-20260818-ABCD1234", OrderStatus.DELIVERED, null, admin);

            assertThat(response.getStatus()).isEqualTo("DELIVERED");
            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        }

        @Test
        @DisplayName("Invalid transition (PENDING -> DELIVERED) returns 409 and no mutation")
        void invalidTransition_returns409() {
            stubLockedOrder();

            assertThatThrownBy(() ->
                            orderService.updateOrderStatus("ORD-20260818-ABCD1234", OrderStatus.DELIVERED, null, admin))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ORDER_STATUS_CONFLICT.getMessage());
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Terminal-state transition (CANCELLED -> CONFIRMED) returns 409")
        void terminalStateTransition_returns409() {
            order.setStatus(OrderStatus.CANCELLED);
            stubLockedOrder();

            assertThatThrownBy(() ->
                            orderService.updateOrderStatus("ORD-20260818-ABCD1234", OrderStatus.CONFIRMED, null, admin))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ORDER_STATUS_CONFLICT.getMessage());
            verify(orderStatusHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Missing order returns 404")
        void missingOrder_returns404() {
            when(orderRepository.findByOrderCodeForUpdate("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus("NOPE", OrderStatus.CONFIRMED, null, admin))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
        }
    }

    @Test
    @DisplayName("Admin search rejects an unknown status instead of dropping the filter")
    void adminSearch_invalidStatus_returns400Error() {
        AdminOrderSearchRequest request =
                AdminOrderSearchRequest.builder().status("NOT_A_STATUS").build();

        assertThatThrownBy(() -> orderService.getAdminOrders(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INVALID_KEY.getMessage());
    }

    @Test
    @DisplayName("Admin search rejects negative page and size")
    void adminSearch_negativePageOrSize_returns400Error() {
        AdminOrderSearchRequest negativePage =
                AdminOrderSearchRequest.builder().page(-1).build();
        AdminOrderSearchRequest zeroSize =
                AdminOrderSearchRequest.builder().size(0).build();

        assertThatThrownBy(() -> orderService.getAdminOrders(negativePage))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INVALID_KEY.getMessage());
        assertThatThrownBy(() -> orderService.getAdminOrders(zeroSize))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INVALID_KEY.getMessage());
    }
}
