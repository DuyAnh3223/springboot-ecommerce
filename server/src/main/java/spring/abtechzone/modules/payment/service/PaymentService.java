package spring.abtechzone.modules.payment.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.constant.PaymentStatus;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.dto.PaymentSummary;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentService {

    static final String INITIAL_IDEMPOTENCY_KEY = "initial";

    PaymentRepository paymentRepository;

    @Transactional
    public Payment createInitialPayment(Order order, PaymentMethod method) {
        if (order == null || order.getId() == null || method == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        return paymentRepository
                .findByOrderIdAndIdempotencyKey(order.getId(), INITIAL_IDEMPOTENCY_KEY)
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .order(order)
                        .provider(providerFor(method))
                        .method(method)
                        .status(PaymentAttemptStatus.PENDING)
                        .amount(order.getTotalAmount())
                        .currency(order.getCurrency())
                        .idempotencyKey(INITIAL_IDEMPOTENCY_KEY)
                        .build()));
    }

    @Transactional(readOnly = true)
    public Payment getInitialPayment(Order order) {
        return paymentRepository
                .findByOrderIdAndIdempotencyKey(order.getId(), INITIAL_IDEMPOTENCY_KEY)
                .orElseThrow(() -> new AppException(ErrorCode.SYSTEM_ERROR));
    }

    @Transactional(readOnly = true)
    public List<Payment> getAttempts(Order order) {
        List<Payment> payments = paymentRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        if (payments.isEmpty()) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        return payments;
    }

    @Transactional(readOnly = true)
    public Long getOrderIdForPayment(Long paymentId) {
        return paymentRepository
                .findOrderIdByPaymentId(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId, Order order) {
        return paymentRepository
                .findByIdAndOrderId(paymentId, order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Map<Long, PaymentSummary> getSummaries(Collection<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        Map<Long, Order> orderById = orders.stream()
                .filter(order -> order.getId() != null)
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        List<Payment> payments = paymentRepository.findByOrderIdInOrderByCreatedAtAscIdAsc(orderById.keySet());
        Map<Long, List<Payment>> paymentsByOrderId = payments.stream()
                .collect(Collectors.groupingBy(payment -> payment.getOrder().getId()));
        Map<Long, PaymentSummary> summaries = new HashMap<>();
        for (Order order : orderById.values()) {
            List<Payment> orderPayments = paymentsByOrderId.get(order.getId());
            if (orderPayments == null || orderPayments.isEmpty()) {
                throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
            }
            summaries.put(order.getId(), summaryFor(order, orderPayments));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public PaymentSummary getSummary(Order order) {
        return getSummaries(List.of(order)).get(order.getId());
    }

    @Transactional
    public void markCodSucceeded(Order order) {
        Payment payment = getInitialPayment(order);
        if (payment.getMethod() != PaymentMethod.COD) {
            return;
        }
        if (payment.getStatus() == PaymentAttemptStatus.SUCCEEDED) {
            return;
        }
        if (payment.getStatus() != PaymentAttemptStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
        }
        payment.setStatus(PaymentAttemptStatus.SUCCEEDED);
        payment.setPaidAt(OffsetDateTime.now());
    }

    @Transactional
    public void cancelPendingPayments(Order order) {
        for (Payment payment : getAttempts(order)) {
            if (payment.getStatus() == PaymentAttemptStatus.PENDING) {
                payment.setStatus(PaymentAttemptStatus.CANCELLED);
            }
        }
    }

    @Transactional
    public Payment retryMockPayment(Order order, String idempotencyKey) {
        Payment existing = paymentRepository
                .findByOrderIdAndIdempotencyKey(order.getId(), idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        List<Payment> payments = getAttempts(order);
        Payment initial = payments.getFirst();
        if (initial.getMethod() != PaymentMethod.MOCK
                || order.getStatus() != OrderStatus.PENDING
                || payments.stream().anyMatch(payment -> payment.getStatus() == PaymentAttemptStatus.SUCCEEDED)
                || payments.stream().anyMatch(payment -> payment.getStatus() == PaymentAttemptStatus.PENDING)) {
            throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
        }
        return paymentRepository.save(Payment.builder()
                .order(order)
                .provider(PaymentProvider.MOCK)
                .method(PaymentMethod.MOCK)
                .status(PaymentAttemptStatus.PENDING)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .idempotencyKey(idempotencyKey)
                .build());
    }

    @Transactional
    public boolean applyMockResult(
            Payment payment,
            PaymentAttemptStatus outcome,
            String providerReference,
            BigDecimal amount,
            String currency) {
        if (payment.getMethod() != PaymentMethod.MOCK
                || payment.getProvider() != PaymentProvider.MOCK
                || (outcome != PaymentAttemptStatus.SUCCEEDED && outcome != PaymentAttemptStatus.FAILED)) {
            throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
        }
        validateExpectedMoney(payment, amount, currency);
        if (providerReference == null || providerReference.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        String normalizedReference = providerReference.trim();
        Payment referenceOwner = paymentRepository
                .findByProviderAndProviderReference(PaymentProvider.MOCK, normalizedReference)
                .orElse(null);
        if (referenceOwner != null && !referenceOwner.getId().equals(payment.getId())) {
            throw new AppException(ErrorCode.PAYMENT_REFERENCE_CONFLICT);
        }
        if (payment.getStatus() != PaymentAttemptStatus.PENDING) {
            if (payment.getStatus() == outcome && Objects.equals(payment.getProviderReference(), normalizedReference)) {
                return false;
            }
            throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
        }
        payment.setProviderReference(normalizedReference);
        payment.setStatus(outcome);
        payment.setRawPayload("MOCK:" + outcome.name());
        if (outcome == PaymentAttemptStatus.SUCCEEDED) {
            payment.setPaidAt(OffsetDateTime.now());
        }
        return true;
    }

    @Transactional
    public void flush() {
        paymentRepository.flush();
    }

    private PaymentSummary summaryFor(Order order, List<Payment> payments) {
        PaymentMethod method = payments.getFirst().getMethod();
        if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentAttemptStatus.SUCCEEDED)) {
            return new PaymentSummary(method, PaymentStatus.PAID);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return new PaymentSummary(method, PaymentStatus.CANCELLED);
        }
        return new PaymentSummary(method, PaymentStatus.UNPAID);
    }

    private void validateExpectedMoney(Payment payment, BigDecimal amount, String currency) {
        if (amount == null
                || amount.compareTo(payment.getAmount()) != 0
                || !payment.getCurrency().equals(currency)) {
            throw new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private PaymentProvider providerFor(PaymentMethod method) {
        return method == PaymentMethod.COD ? PaymentProvider.INTERNAL : PaymentProvider.MOCK;
    }
}
