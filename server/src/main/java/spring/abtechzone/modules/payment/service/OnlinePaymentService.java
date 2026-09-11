package spring.abtechzone.modules.payment.service;

import java.net.URI;
import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.*;
import spring.abtechzone.modules.order.constant.*;
import spring.abtechzone.modules.order.entity.*;
import spring.abtechzone.modules.order.repository.*;
import spring.abtechzone.modules.order.service.OrderLifecycleService;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.*;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.gateway.*;
import spring.abtechzone.modules.payment.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class OnlinePaymentService {
    public enum Applied {
        RECORDED,
        DUPLICATE,
        UNKNOWN,
        MISMATCH,
        CONFLICT
    }

    public record CheckoutView(
            Long paymentId,
            String checkoutUrl,
            String status,
            String initiationState,
            String applicationStatus,
            String reviewReason,
            OffsetDateTime deadline) {}

    private record Work(
            Long paymentId, PaymentProvider provider, String token, PaymentGateway.Attempt attempt, boolean create) {}

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderStatusHistoryRepository histories;
    private final OrderLifecycleService lifecycle;
    private final GatewayRegistry gateways;
    private final OnlinePaymentProperties properties;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final jakarta.persistence.EntityManager entityManager;
    private final spring.abtechzone.modules.payment.repository.PaymentCheckoutKeyRepository checkoutKeys;

    public void requireProvider(PaymentProvider provider) {
        gateways.require(provider);
    }

    @Transactional
    public Payment createInitial(Order order, PaymentProvider provider) {
        gateways.validateMoney(provider, order.getTotalAmount(), order.getCurrency());
        return newAttempt(order, provider, "initial", now().plusMinutes(properties.getDeadlineMinutes()));
    }

    private Payment newAttempt(Order order, PaymentProvider provider, String key, OffsetDateTime deadline) {
        Payment p = Payment.builder()
                .order(order)
                .provider(provider)
                .method(PaymentMethod.ONLINE)
                .status(PaymentAttemptStatus.PENDING)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .idempotencyKey(key)
                .createdAt(now())
                .paymentDeadline(deadline)
                .initiationState("NEW")
                .applicationStatus("NONE")
                .queryCount(0)
                .build();
        payments.saveAndFlush(p);
        // DB-generated identity is stable, unique, numeric and within payOS' safe integer range.
        if (p.getId() > 9_007_199_254_740_991L) throw new AppException(ErrorCode.SYSTEM_ERROR);
        p.setMerchantRequestId(p.getId().toString());
        return p;
    }

    public CheckoutView checkout(String code, String rawKey, PaymentProvider provider, UUID userId, String ip) {
        gateways.require(provider);
        String key;
        try {
            key = UUID.fromString(rawKey).toString();
        } catch (RuntimeException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        Long id = transactions.execute(tx -> {
            Order order = orders.findByOrderCodeForUpdate(code)
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            if (!order.getUserId().equals(userId)) throw new AppException(ErrorCode.ORDER_NOT_FOUND);
            List<Payment> attempts = payments.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
            Payment initial = attempts.getFirst();
            if (initial.getMethod() != PaymentMethod.ONLINE) throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
            Payment replay = checkoutKeys
                    .findByOrderIdAndRequestKey(order.getId(), key)
                    .map(spring.abtechzone.modules.payment.entity.PaymentCheckoutKey::getPayment)
                    .orElse(null);
            if (replay == null)
                replay = attempts.stream()
                        .filter(p -> p.getIdempotencyKey().equals(key))
                        .findFirst()
                        .orElse(null);
            if (replay != null) {
                if (replay.getProvider() != provider) throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
                return receipt(order, key, replay);
            }
            if (order.getStatus() != OrderStatus.PENDING
                    || !now().isBefore(initial.getPaymentDeadline())
                    || attempts.stream()
                            .anyMatch(p -> p.getStatus() == PaymentAttemptStatus.SUCCEEDED
                                    || "REVIEW_REQUIRED".equals(p.getApplicationStatus())))
                throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
            Payment active = attempts.stream()
                    .filter(p -> p.getStatus() == PaymentAttemptStatus.PENDING)
                    .findFirst()
                    .orElse(null);
            if (active != null) {
                if (active.getProvider() != provider) throw new AppException(ErrorCode.PAYMENT_STATE_CONFLICT);
                return receipt(order, key, active);
            }
            gateways.validateMoney(provider, order.getTotalAmount(), order.getCurrency());
            return receipt(order, key, newAttempt(order, provider, key, initial.getPaymentDeadline()));
        });
        Work work = claim(id, ip, false);
        if (work != null) execute(work);
        return transactions.execute(tx -> view(locked(id)));
    }

    private Work claim(Long id, String ip, boolean worker) {
        return transactions.execute(tx -> {
            Payment p = locked(id);
            if (p.getStatus() == PaymentAttemptStatus.SUCCEEDED || "REVIEW_REQUIRED".equals(p.getApplicationStatus()))
                return null;
            if (p.getLeaseUntil() != null && now().isBefore(p.getLeaseUntil())) return null;
            boolean create = "NEW".equals(p.getInitiationState());
            if (create
                    && (worker
                            || p.getOrder().getStatus() != OrderStatus.PENDING
                            || !now().isBefore(p.getPaymentDeadline()))) return null;
            if (!worker && !create) return null; // user retries cannot bypass reconciliation backoff
            if (p.getNextQueryAt() != null && worker && now().isBefore(p.getNextQueryAt())) return null;
            if (p.getClientIp() == null) p.setClientIp(ip == null ? "127.0.0.1" : ip);
            String token = UUID.randomUUID().toString();
            p.setLeaseUntil(now().plusSeconds(90));
            p.setLeaseToken(token);
            if (create) p.setInitiationState("UNKNOWN");
            p.setNextQueryAt(now().plusSeconds(90));
            return new Work(p.getId(), p.getProvider(), token, snapshot(p), create);
        });
    }

    private Long receipt(Order order, String key, Payment payment) {
        if (checkoutKeys.findByOrderIdAndRequestKey(order.getId(), key).isEmpty()) {
            var receipt = new spring.abtechzone.modules.payment.entity.PaymentCheckoutKey();
            receipt.setOrderId(order.getId());
            receipt.setRequestKey(key);
            receipt.setPayment(payment);
            checkoutKeys.save(receipt);
        }
        return payment.getId();
    }

    private void execute(Work work) {
        try {
            var gateway = gateways.require(work.provider());
            if (work.create()) {
                var checkout = gateway.create(work.attempt());
                validateCheckoutUrl(work.provider(), checkout.url());
                transactions.executeWithoutResult(tx -> {
                    Payment p = locked(work.paymentId());
                    if (!work.token().equals(p.getLeaseToken())) return;
                    if (p.getStatus() == PaymentAttemptStatus.PENDING
                            && !"REVIEW_REQUIRED".equals(p.getApplicationStatus())) {
                        p.setCheckoutUrl(checkout.url());
                        p.setProviderCheckoutId(checkout.providerId());
                        p.setInitiationState("READY");
                    }
                    release(p);
                    p.setNextQueryAt(p.getStatus() == PaymentAttemptStatus.SUCCEEDED ? null : now().plusSeconds(60));
                });
            } else {
                var result = gateway.query(work.attempt());
                if (!work.attempt().merchantId().equals(result.merchantId()))
                    throw new GatewayException(GatewayException.Kind.DATA);
                apply(work.provider(), result);
                finishQuery(work, false);
            }
        } catch (AppException e) {
            finishQuery(work, false);
            throw e;
        } catch (GatewayException e) {
            finishQuery(work, e.kind() == GatewayException.Kind.DATA || e.kind() == GatewayException.Kind.SIGNATURE);
        } catch (IllegalArgumentException e) {
            finishQuery(work, true);
        }
    }

    private void finishQuery(Work work, boolean invalidData) {
        transactions.executeWithoutResult(tx -> {
            Payment p = locked(work.paymentId());
            if (!work.token().equals(p.getLeaseToken())) return;
            release(p);
            if (p.getStatus() == PaymentAttemptStatus.SUCCEEDED || "REVIEW_REQUIRED".equals(p.getApplicationStatus())) {
                p.setNextQueryAt(null);
                return;
            }
            int count = (p.getQueryCount() == null ? 0 : p.getQueryCount()) + 1;
            p.setQueryCount(count);
            if (invalidData
                    || count >= properties.getMaxQueries()
                    || now().isAfter(p.getCreatedAt().plusHours(properties.getQueryWindowHours()))) {
                p.setApplicationStatus("REVIEW_REQUIRED");
                p.setReviewReason(invalidData ? "GATEWAY_DATA_MISMATCH" : "RECONCILIATION_EXHAUSTED");
                p.setNextQueryAt(null);
            } else if (p.getStatus() == PaymentAttemptStatus.FAILED) p.setNextQueryAt(null);
            else p.setNextQueryAt(now().plusSeconds(Math.min(900, 30L << Math.min(count, 5))));
        });
    }

    public Applied apply(PaymentProvider provider, PaymentGateway.Result result) {
        // Lookup only; entity is reloaded after locking Order inside the transaction.
        Long id = payments.findIdByMerchant(provider, result.merchantId()).orElse(null);
        if (id == null) return Applied.UNKNOWN;
        return transactions.execute(tx -> {
            Payment p = locked(id);
            Order order = p.getOrder();
            // Whitelist only reconciliation evidence; never retain signature, account or customer payload.
            var evidence = GatewayCrypto.JSON.createObjectNode();
            evidence.put("amount", result.amount());
            evidence.put("currency", result.currency());
            evidence.put("outcome", result.outcome().name());
            evidence.put("transactionId", result.transactionId());
            evidence.put("merchantId", result.merchantId());
            if (p.getStatus() != PaymentAttemptStatus.SUCCEEDED
                    || !Objects.equals(p.getProviderReference(), result.transactionId()))
                p.setRawPayload(evidence.toString());
            if (p.getAmount().compareTo(result.amount()) != 0
                    || !p.getCurrency().equals(result.currency())
                    || (p.getProviderCheckoutId() != null
                            && result.providerId() != null
                            && !p.getProviderCheckoutId().equals(result.providerId()))) {
                p.setReviewReason("CALLBACK_AMOUNT_OR_ID_MISMATCH");
                p.setNextQueryAt(null);
                if (p.getAppliedOrderId() == null) p.setApplicationStatus("REVIEW_REQUIRED");
                return Applied.MISMATCH;
            }
            if (result.outcome() == PaymentGateway.Outcome.PENDING) return Applied.RECORDED;
            if (result.outcome() == PaymentGateway.Outcome.FAILED) {
                if (p.getStatus() == PaymentAttemptStatus.SUCCEEDED) return Applied.DUPLICATE;
                p.setStatus(PaymentAttemptStatus.FAILED);
                p.setInitiationState("RESOLVED");
                p.setNextQueryAt(null);
                return Applied.RECORDED;
            }
            String reference = result.transactionId();
            if (reference == null || reference.isBlank() || reference.length() > 150 || "0".equals(reference)) {
                p.setReviewReason("INVALID_TRANSACTION_REFERENCE");
                p.setNextQueryAt(null);
                if (p.getAppliedOrderId() == null) p.setApplicationStatus("REVIEW_REQUIRED");
                return Applied.MISMATCH;
            }
            Payment owner = payments.findByProviderAndProviderReference(provider, reference)
                    .orElse(null);
            if (owner != null && !owner.getId().equals(p.getId())) {
                p.setReviewReason("TRANSACTION_REFERENCE_CONFLICT");
                p.setNextQueryAt(null);
                if (p.getAppliedOrderId() == null) p.setApplicationStatus("REVIEW_REQUIRED");
                return Applied.CONFLICT;
            }
            if (p.getStatus() == PaymentAttemptStatus.SUCCEEDED) {
                if (reference.equals(p.getProviderReference())) return Applied.DUPLICATE;
                p.setReviewReason("ADDITIONAL_TRANSACTION:" + reference.substring(0, Math.min(70, reference.length())));
                return Applied.CONFLICT;
            }
            boolean applied = payments.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId()).stream()
                    .anyMatch(a -> a.getAppliedOrderId() != null);
            String review = OnlinePaymentRules.reviewReason(order.getStatus(), p.getPaymentDeadline(), now(), applied);
            if (review != null && order.getStatus() == OrderStatus.PENDING && !now().isBefore(p.getPaymentDeadline()))
                lifecycle.expireOnlineOrder(order);
            p.setStatus(PaymentAttemptStatus.SUCCEEDED);
            p.setPaidAt(now());
            p.setProviderReference(reference);
            p.setInitiationState("RESOLVED");
            p.setNextQueryAt(null);
            p.setReviewReason(review);
            p.setApplicationStatus(review == null ? "APPLIED" : "REVIEW_REQUIRED");
            if (review == null) {
                p.setAppliedOrderId(order.getId());
                order.setStatus(OrderStatus.CONFIRMED);
                var history = new OrderStatusHistory();
                history.setOrder(order);
                history.setStatus("CONFIRMED");
                history.setFromStatus("PENDING");
                history.setToStatus("CONFIRMED");
                history.setActorType("SYSTEM");
                history.setNote("Verified " + provider + " payment");
                history.setCreatedAt(now());
                histories.save(history);
            }
            payments.flush();
            return Applied.RECORDED;
        });
    }

    public void reconcile(Long id) {
        Work work = claim(id, null, true);
        if (work != null) execute(work);
    }

    public void expire(Long id) {
        transactions.executeWithoutResult(tx -> {
            Payment p = locked(id);
            if (p.getPaymentDeadline() != null && !now().isBefore(p.getPaymentDeadline()))
                lifecycle.expireOnlineOrder(p.getOrder());
        });
    }

    private Payment locked(Long id) {
        Long orderId =
                payments.findOrderIdByPaymentId(id).orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Order order = orders.findByIdForUpdate(orderId).orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        // OSIV can retain objects across transaction boundaries during provider HTTP calls.
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        Payment payment = payments.findById(id).orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        entityManager.refresh(payment);
        return payment;
    }

    private PaymentGateway.Attempt snapshot(Payment p) {
        return new PaymentGateway.Attempt(
                p.getMerchantRequestId(),
                p.getAmount(),
                p.getCurrency(),
                p.getOrder().getOrderCode(),
                p.getCreatedAt(),
                p.getPaymentDeadline(),
                p.getClientIp());
    }

    private CheckoutView view(Payment p) {
        String url = p.getStatus() == PaymentAttemptStatus.PENDING
                        && p.getOrder().getStatus() == OrderStatus.PENDING
                        && now().isBefore(p.getPaymentDeadline())
                        && !"REVIEW_REQUIRED".equals(p.getApplicationStatus())
                ? p.getCheckoutUrl()
                : null;
        return new CheckoutView(
                p.getId(),
                url,
                p.getStatus().name(),
                p.getInitiationState(),
                p.getApplicationStatus(),
                p.getReviewReason(),
                p.getPaymentDeadline());
    }

    private void release(Payment p) {
        p.setLeaseToken(null);
        p.setLeaseUntil(null);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private void validateCheckoutUrl(PaymentProvider provider, String raw) {
        URI uri;
        try {
            uri = URI.create(raw);
        } catch (RuntimeException e) {
            throw new GatewayException(GatewayException.Kind.DATA);
        }
        String host = uri.getHost();
        String domain =
                switch (provider) {
                    case MOMO -> "momo.vn";
                    case VNPAY -> "vnpayment.vn";
                    case PAYOS -> "payos.vn";
                    default -> "";
                };
        if (!"https".equals(uri.getScheme())
                || host == null
                || uri.getUserInfo() != null
                || !(host.equals(domain) || host.endsWith("." + domain)))
            throw new GatewayException(GatewayException.Kind.DATA);
    }
}
