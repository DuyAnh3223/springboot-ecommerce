package spring.abtechzone.modules.order.service;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.dto.request.AdminOrderSearchRequest;
import spring.abtechzone.modules.order.dto.response.OrderDetailResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.dto.response.OrderSummaryResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderItem;
import spring.abtechzone.modules.order.entity.OrderStatusHistory;
import spring.abtechzone.modules.order.mapper.OrderMapper;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.order.repository.specification.OrderSpecifications;
import spring.abtechzone.modules.order.service.OrderTransitionPolicy.Actor;
import spring.abtechzone.modules.payment.dto.PaymentSummary;
import spring.abtechzone.modules.payment.service.PaymentService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;
import spring.abtechzone.modules.voucher.repository.VoucherRedemptionRepository;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderLifecycleService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String CREATED_AT = "createdAt";

    OrderRepository orderRepository;
    OrderStatusHistoryRepository orderStatusHistoryRepository;
    VoucherRepository voucherRepository;
    VoucherRedemptionRepository voucherRedemptionRepository;
    InventoryService inventoryService;
    StockMovementRepository stockMovementRepository;
    OrderMapper orderMapper;
    PaymentService paymentService;

    /** Called with the Order row locked; joins the result/worker transaction. */
    @Transactional
    public void expireOnlineOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) return;
        boolean applied = paymentService.getAttempts(order).stream().anyMatch(p -> p.getAppliedOrderId() != null);
        if (applied) return;
        compensateCancellation(order);
        paymentService.cancelPendingPayments(order);
        order.setStatus(OrderStatus.CANCELLED);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus("CANCELLED");
        history.setFromStatus("PENDING");
        history.setToStatus("CANCELLED");
        history.setActorType("SYSTEM");
        history.setNote("Online payment deadline expired");
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size, User user) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Pageable pageable =
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(CREATED_AT).descending());
        Page<Order> orders = status == null
                ? orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status, pageable);
        var paymentSummaries = paymentService.getSummaries(orders.getContent());
        return orders.map(order -> toSummary(order, Actor.CUSTOMER, paymentSummaries.get(order.getId())));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrderDetail(String orderCode, User user) {
        Order order = orderRepository
                .findWithItemsByOrderCodeAndUserId(orderCode, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        OrderDetailResponse detail = toDetail(order, Actor.CUSTOMER, paymentService.getSummary(order));
        detail.setHistory(orderStatusHistoryRepository.findByOrderIdOrdered(order.getId()).stream()
                .map(orderMapper::toOrderHistoryResponse)
                .toList());
        return detail;
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getAdminOrderDetail(String orderCode) {
        Order order = orderRepository
                .findWithItemsByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        OrderDetailResponse detail = toDetail(order, Actor.ADMIN, paymentService.getSummary(order));
        detail.setHistory(orderStatusHistoryRepository.findByOrderIdOrdered(order.getId()).stream()
                .map(orderMapper::toOrderHistoryResponse)
                .toList());
        return detail;
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAdminOrders(AdminOrderSearchRequest request) {
        if (request.getPage() < 0 || request.getSize() < 1) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        OrderStatus status = parseStatus(request.getStatus());
        if (request.getFromDate() != null
                && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        int safeSize = Math.clamp(request.getSize(), 1, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                Math.max(request.getPage(), 0), safeSize, Sort.by(CREATED_AT).descending());
        Specification<Order> spec = OrderSpecifications.adminSearch(
                request.getSearch(), status, request.getFromDate(), request.getToDate());
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        var paymentSummaries = paymentService.getSummaries(orders.getContent());
        return orders.map(order -> toSummary(order, Actor.ADMIN, paymentSummaries.get(order.getId())));
    }

    @Transactional
    public OrderResponse cancelOrder(String orderCode, String reason, User user) {
        Order order = orderRepository
                .findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toOrderResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        PaymentSummary payment = paymentService.getSummary(order);
        if (!OrderTransitionPolicy.isAllowed(order.getStatus(), OrderStatus.CANCELLED, Actor.CUSTOMER, payment)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        compensateCancellation(order);
        paymentService.cancelPendingPayments(order);
        applyTransition(
                order, OrderStatus.CANCELLED, Actor.CUSTOMER, user.getId().toString(), normalizeNote(reason), payment);
        orderRepository.save(order);
        orderRepository.flush();
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderCode, OrderStatus target, String note, User admin) {
        Order order = orderRepository
                .findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        PaymentSummary payment = paymentService.getSummary(order);
        if (order.getStatus() != target
                && !OrderTransitionPolicy.isAllowed(order.getStatus(), target, Actor.ADMIN, payment)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        if (order.getStatus() != OrderStatus.CANCELLED && target == OrderStatus.CANCELLED) {
            compensateCancellation(order);
            paymentService.cancelPendingPayments(order);
        } else if (target == OrderStatus.DELIVERED) {
            paymentService.markCodSucceeded(order);
        }
        applyTransition(order, target, Actor.ADMIN, admin.getId().toString(), normalizeNote(note), payment);
        orderRepository.save(order);
        orderRepository.flush();
        return orderMapper.toOrderResponse(order);
    }

    private OrderSummaryResponse toSummary(Order order, Actor actor, PaymentSummary payment) {
        OrderSummaryResponse summary = orderMapper.toOrderSummaryResponse(order);
        applyPaymentSummary(summary, payment);
        summary.setAllowedTransitions(
                OrderTransitionPolicy.allowedTransitions(order.getStatus(), actor, payment).stream()
                        .map(Enum::name)
                        .toList());
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            summary.setPreviewItem(
                    orderMapper.toOrderItemResponse(order.getItems().getFirst()));
        }
        return summary;
    }

    private OrderDetailResponse toDetail(Order order, Actor actor, PaymentSummary payment) {
        OrderDetailResponse detail = orderMapper.toOrderDetailResponse(order);
        detail.setPaymentMethod(payment.method().name());
        detail.setPaymentStatus(payment.status().name());
        detail.setAllowedTransitions(
                OrderTransitionPolicy.allowedTransitions(order.getStatus(), actor, payment).stream()
                        .map(Enum::name)
                        .toList());
        detail.setItems(orderMapper.toOrderItemResponses(order.getItems()));
        return detail;
    }

    private void applyPaymentSummary(OrderSummaryResponse response, PaymentSummary payment) {
        response.setPaymentMethod(payment.method().name());
        response.setPaymentStatus(payment.status().name());
    }

    private OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private void compensateCancellation(Order order) {
        restoreCancellationStock(order);
        reverseCancellationVoucher(order);
    }

    private void restoreCancellationStock(Order order) {
        if (order.getItems() == null) {
            return;
        }
        order.getItems().forEach(item -> restoreCancellationStockItem(order, item));
    }

    private void restoreCancellationStockItem(Order order, OrderItem item) {
        validateCancellationItem(item);
        Long skuId = item.getSkuId();
        if (stockMovementRepository != null
                && stockMovementRepository.existsBySkuIdAndReasonAndReferenceId(
                        skuId, StockMovementReason.ORDER_CANCEL_RETURN, String.valueOf(order.getId()))) {
            return;
        }
        inventoryService.increaseStock(skuId, item.getQuantity(), order, item.getSku());
    }

    private void validateCancellationItem(OrderItem item) {
        if (item.getSkuId() == null || item.getQuantity() <= 0) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private void reverseCancellationVoucher(Order order) {
        if (!isVoucherBearing(order)) {
            return;
        }
        if (order.getVoucher() == null || order.getVoucher().getId() == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        int reversed = voucherRedemptionRepository.reverseRedemptionByOrderId(order.getId());
        if (reversed == 0) {
            if (voucherRedemptionRepository.existsByOrderIdAndStatus(order.getId(), VoucherRedemptionStatus.REVERSED)) {
                return;
            }
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        requireSingleUpdatedRow(
                voucherRepository.decreaseUsedCount(order.getVoucher().getId()));
    }

    private boolean isVoucherBearing(Order order) {
        return order.getVoucher() != null
                || (order.getVoucherCode() != null && !order.getVoucherCode().isBlank());
    }

    private void requireSingleUpdatedRow(int updatedRows) {
        if (updatedRows != 1) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyTransition(
            Order order, OrderStatus target, Actor actor, String actorId, String note, PaymentSummary payment) {
        if (order.getStatus() == target) {
            return;
        }
        OrderStatus from = order.getStatus();
        if (OrderTransitionPolicy.isTerminal(from)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        if (!OrderTransitionPolicy.isAllowed(from, target, actor, payment)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        order.setStatus(target);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(target.name());
        history.setFromStatus(from.name());
        history.setToStatus(target.name());
        history.setActorType(actor.name());
        history.setActorId(actorId);
        history.setNote(note);
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);
    }
}
