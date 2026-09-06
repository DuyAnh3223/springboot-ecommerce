package spring.abtechzone.modules.payment.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderStatusHistory;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.payment.config.PaymentMockPolicy;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.dto.PaymentSummary;
import spring.abtechzone.modules.payment.dto.request.MockPaymentResultRequest;
import spring.abtechzone.modules.payment.dto.response.OrderPaymentsResponse;
import spring.abtechzone.modules.payment.dto.response.PaymentAttemptResponse;
import spring.abtechzone.modules.payment.dto.response.PaymentCommandResponse;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.mapper.PaymentMapper;
import spring.abtechzone.modules.user.entity.User;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderPaymentService {

    OrderRepository orderRepository;
    OrderStatusHistoryRepository orderStatusHistoryRepository;
    PaymentService paymentService;
    PaymentMapper paymentMapper;
    PaymentMockPolicy mockPolicy;
    TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public OrderPaymentsResponse getMyPayments(String orderCode, User user) {
        Order order = orderRepository
                .findByOrderCodeAndUserId(orderCode, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toOverview(order);
    }

    @Transactional(readOnly = true)
    public OrderPaymentsResponse getAdminPayments(String orderCode) {
        Order order = orderRepository
                .findByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return toOverview(order);
    }

    public PaymentCommandResponse retryMock(String orderCode, String rawIdempotencyKey, User user) {
        mockPolicy.requireEnabled();
        String idempotencyKey = canonicalUuid(rawIdempotencyKey);
        PaymentCommandResponse response =
                transactionTemplate.execute(status -> doRetryMock(orderCode, idempotencyKey, user));
        if (response == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        return response;
    }

    private PaymentCommandResponse doRetryMock(String orderCode, String idempotencyKey, User user) {
        Order order = orderRepository
                .findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        Payment payment = paymentService.retryMockPayment(order, idempotencyKey);
        paymentService.flush();
        return toCommand(order, payment);
    }

    public PaymentCommandResponse applyMockResult(Long paymentId, MockPaymentResultRequest request, User admin) {
        mockPolicy.requireEnabled();
        Long orderId = paymentService.getOrderIdForPayment(paymentId);
        try {
            PaymentCommandResponse response =
                    transactionTemplate.execute(status -> doApplyMockResult(orderId, paymentId, request, admin));
            if (response == null) {
                throw new AppException(ErrorCode.SYSTEM_ERROR);
            }
            return response;
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.PAYMENT_REFERENCE_CONFLICT);
        }
    }

    private PaymentCommandResponse doApplyMockResult(
            Long orderId, Long paymentId, MockPaymentResultRequest request, User admin) {
        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentService.getPayment(paymentId, order);

        if (payment.getStatus() == PaymentAttemptStatus.PENDING && order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
        }
        boolean changed = paymentService.applyMockResult(
                payment,
                request.getOutcome(),
                request.getProviderReference(),
                request.getAmount(),
                request.getCurrency());
        if (changed && request.getOutcome() == PaymentAttemptStatus.SUCCEEDED) {
            order.setStatus(OrderStatus.CONFIRMED);
            createHistory(order, OrderStatus.PENDING, admin);
        }
        orderRepository.flush();
        paymentService.flush();
        return toCommand(order, payment);
    }

    private OrderPaymentsResponse toOverview(Order order) {
        PaymentSummary summary = paymentService.getSummary(order);
        List<PaymentAttemptResponse> attempts = paymentService.getAttempts(order).stream()
                .map(paymentMapper::toResponse)
                .toList();
        return OrderPaymentsResponse.builder()
                .orderCode(order.getOrderCode())
                .orderStatus(order.getStatus().name())
                .paymentMethod(summary.method().name())
                .paymentStatus(summary.status().name())
                .retryAllowed(order.getStatus() == OrderStatus.PENDING
                        && attempts.stream()
                                .allMatch(
                                        p -> PaymentAttemptStatus.FAILED.name().equals(p.getStatus()))
                        && attempts.stream().noneMatch(p -> "REVIEW_REQUIRED".equals(p.getApplicationStatus()))
                        && attempts.getFirst().getPaymentDeadline() != null
                        && OffsetDateTime.now().isBefore(attempts.getFirst().getPaymentDeadline()))
                .attempts(attempts)
                .build();
    }

    private PaymentCommandResponse toCommand(Order order, Payment payment) {
        return PaymentCommandResponse.builder()
                .payment(paymentMapper.toResponse(payment))
                .orderCode(order.getOrderCode())
                .orderStatus(order.getStatus().name())
                .paymentStatus(paymentService.getSummary(order).status().name())
                .build();
    }

    private void createHistory(Order order, OrderStatus from, User actor) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.CONFIRMED.name());
        history.setFromStatus(from.name());
        history.setToStatus(OrderStatus.CONFIRMED.name());
        history.setActorType("ADMIN");
        history.setActorId(actor.getId().toString());
        history.setNote("Mock payment succeeded");
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private String canonicalUuid(String raw) {
        try {
            return UUID.fromString(raw == null ? "" : raw.trim()).toString();
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
