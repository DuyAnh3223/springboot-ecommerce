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
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentStatus;
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
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
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
    ProductSkuRepository productSkuRepository;
    VoucherRepository voucherRepository;
    VoucherRedemptionRepository voucherRedemptionRepository;
    StockMovementRepository stockMovementRepository;
    OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size, User user) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Pageable pageable =
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(CREATED_AT).descending());
        Page<Order> orders = status == null
                ? orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status, pageable);
        return orders.map(order -> toSummary(order, Actor.CUSTOMER));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrderDetail(String orderCode, User user) {
        Order order = orderRepository
                .findWithItemsByOrderCodeAndUserId(orderCode, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        OrderDetailResponse detail = toDetail(order, Actor.CUSTOMER);
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
        OrderDetailResponse detail = toDetail(order, Actor.ADMIN);
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
        return orderRepository.findAll(spec, pageable).map(order -> toSummary(order, Actor.ADMIN));
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
        compensateCancellation(order);
        applyTransition(
                order, OrderStatus.CANCELLED, Actor.CUSTOMER, user.getId().toString(), normalizeNote(reason));
        orderRepository.save(order);
        orderRepository.flush();
        return orderMapper.toOrderResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderCode, OrderStatus target, String note, User admin) {
        Order order = orderRepository
                .findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.CANCELLED && target == OrderStatus.CANCELLED) {
            compensateCancellation(order);
        }
        applyTransition(order, target, Actor.ADMIN, admin.getId().toString(), normalizeNote(note));
        orderRepository.save(order);
        orderRepository.flush();
        return orderMapper.toOrderResponse(order);
    }

    private OrderSummaryResponse toSummary(Order order, Actor actor) {
        OrderSummaryResponse summary = orderMapper.toOrderSummaryResponse(order);
        summary.setAllowedTransitions(OrderTransitionPolicy.allowedTransitions(order.getStatus(), actor).stream()
                .map(Enum::name)
                .toList());
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            summary.setPreviewItem(
                    orderMapper.toOrderItemResponse(order.getItems().getFirst()));
        }
        return summary;
    }

    private OrderDetailResponse toDetail(Order order, Actor actor) {
        OrderDetailResponse detail = orderMapper.toOrderDetailResponse(order);
        detail.setAllowedTransitions(OrderTransitionPolicy.allowedTransitions(order.getStatus(), actor).stream()
                .map(Enum::name)
                .toList());
        detail.setItems(orderMapper.toOrderItemResponses(order.getItems()));
        return detail;
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
        requireSingleUpdatedRow(productSkuRepository.increaseStock(item.getSku().getId(), item.getQuantity()));

        StockMovement movement = new StockMovement();
        movement.setSku(item.getSku());
        movement.setChangeQty(item.getQuantity());
        movement.setReason("ORDER_CANCEL_RETURN");
        movement.setReferenceId(String.valueOf(order.getId()));
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);
    }

    private void validateCancellationItem(OrderItem item) {
        if (item.getSku() == null || item.getSku().getId() == null || item.getQuantity() <= 0) {
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

        requireSingleUpdatedRow(voucherRedemptionRepository.reverseRedemptionByOrderId(order.getId()));
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

    private void applyTransition(Order order, OrderStatus target, Actor actor, String actorId, String note) {
        if (order.getStatus() == target) {
            return;
        }
        OrderStatus from = order.getStatus();
        if (OrderTransitionPolicy.isTerminal(from)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        if (!OrderTransitionPolicy.isAllowed(from, target, actor)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        order.setStatus(target);
        if (target == OrderStatus.CANCELLED) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        } else if (target == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
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
