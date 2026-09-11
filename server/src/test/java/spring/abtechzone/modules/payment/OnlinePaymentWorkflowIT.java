package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.modules.order.constant.*;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.*;
import spring.abtechzone.modules.payment.constant.*;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.gateway.PaymentGateway;
import spring.abtechzone.modules.payment.repository.PaymentRepository;
import spring.abtechzone.modules.payment.service.OnlinePaymentService;

/** Real PostgreSQL proves locking, unique application and atomic rollback, not gateway HTTP. */
class OnlinePaymentWorkflowIT extends BaseIT {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    spring.abtechzone.modules.payment.gateway.GatewayRegistry gateways;

    @Autowired
    jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Autowired
    PaymentRepository payments;

    @Autowired
    OrderRepository orders;

    @Autowired
    OrderStatusHistoryRepository histories;

    @Autowired
    OnlinePaymentService service;

    @Autowired
    TransactionTemplate transactions;

    @BeforeEach
    void clean() {
        payments.deleteAll();
        histories.deleteAll();
        orders.deleteAll();
    }

    @Test
    void concurrentCallbacksConfirmOnce() throws Exception {
        Payment p = payment(order(), "initial");
        race(
                () -> service.apply(p.getProvider(), success(p, "100")),
                () -> service.apply(p.getProvider(), success(p, "100")));
        assertThat(histories.findByOrderIdOrdered(p.getOrder().getId())).hasSize(1);
        assertThat(payments.findById(p.getId()).orElseThrow().getAppliedOrderId())
                .isEqualTo(p.getOrder().getId());
    }

    @Test
    void competingChargesOnlyApplyOneAndPreserveOther() throws Exception {
        Order order = order();
        Payment p = payment(order, "initial"),
                second = payment(order, UUID.randomUUID().toString());
        race(
                () -> service.apply(p.getProvider(), success(p, "101")),
                () -> service.apply(second.getProvider(), success(second, "102")));
        var attempts = payments.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        assertThat(attempts).allMatch(a -> a.getStatus() == PaymentAttemptStatus.SUCCEEDED);
        assertThat(attempts).filteredOn(a -> a.getAppliedOrderId() != null).hasSize(1);
        assertThat(attempts)
                .filteredOn(a -> "REVIEW_REQUIRED".equals(a.getApplicationStatus()))
                .hasSize(1);
        assertThat(histories.findByOrderIdOrdered(order.getId())).hasSize(1);
    }

    @Test
    void expiryAndLateCallbackSerializeAndNeverReopen() throws Exception {
        Payment p = payment(order(), "initial");
        p.setPaymentDeadline(OffsetDateTime.now().minusSeconds(1));
        payments.saveAndFlush(p);
        race(
                () -> {
                    service.expire(p.getId());
                    return null;
                },
                () -> service.apply(p.getProvider(), success(p, "103")));
        assertThat(orders.findById(p.getOrder().getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(payments.findById(p.getId()).orElseThrow().getReviewReason()).isEqualTo("LATE_SUCCESS");
        assertThat(histories.findByOrderIdOrdered(p.getOrder().getId())).hasSize(1);
    }

    @Test
    void outerTransactionRollbackRevertsPaymentOrderAndHistory() {
        Payment p = payment(order(), "initial");
        assertThatThrownBy(() -> transactions.executeWithoutResult(tx -> {
                    service.apply(p.getProvider(), success(p, "104"));
                    throw new IllegalStateException("simulate failure before commit");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
        assertThat(orders.findById(p.getOrder().getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PENDING);
        assertThat(histories.findByOrderIdOrdered(p.getOrder().getId())).isEmpty();
    }

    @Test
    void databaseRejectsTwoAppliedRowsEvenOutsideService() {
        Order o = order();
        Payment p = payment(o, "initial"), second = payment(o, UUID.randomUUID().toString());
        p.setStatus(PaymentAttemptStatus.SUCCEEDED);
        p.setPaidAt(OffsetDateTime.now());
        p.setApplicationStatus("APPLIED");
        p.setAppliedOrderId(o.getId());
        payments.saveAndFlush(p);
        second.setStatus(PaymentAttemptStatus.SUCCEEDED);
        second.setPaidAt(OffsetDateTime.now());
        second.setApplicationStatus("APPLIED");
        second.setAppliedOrderId(o.getId());
        assertThatThrownBy(() -> payments.saveAndFlush(second))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void initialCheckoutKeyReplaysFailedAttemptInsteadOfCreatingAnotherCharge() {
        var gateway = org.mockito.Mockito.mock(PaymentGateway.class);
        org.mockito.Mockito.when(gateways.require(PaymentProvider.MOMO)).thenReturn(gateway);
        Payment p = payment(order(), "initial");
        String key = UUID.randomUUID().toString();
        service.checkout(
                p.getOrder().getOrderCode(), key, p.getProvider(), p.getOrder().getUserId(), "127.0.0.1");
        service.apply(
                p.getProvider(),
                new PaymentGateway.Result(
                        p.getMerchantRequestId(), p.getAmount(), "VND", PaymentGateway.Outcome.FAILED, null, null));
        var replay = service.checkout(
                p.getOrder().getOrderCode(), key, p.getProvider(), p.getOrder().getUserId(), "127.0.0.1");
        assertThat(replay.paymentId()).isEqualTo(p.getId());
        assertThat(replay.status()).isEqualTo("FAILED");
        assertThat(payments.findByOrderIdOrderByCreatedAtAscIdAsc(p.getOrder().getId()))
                .hasSize(1);
        org.mockito.Mockito.verifyNoInteractions(gateway);
    }

    @Test
    void reconciliationRecoversExpiredWorkerLeaseWithoutCreatingPaymentAgain() {
        var gateway = org.mockito.Mockito.mock(PaymentGateway.class);
        org.mockito.Mockito.when(gateways.require(PaymentProvider.MOMO)).thenReturn(gateway);
        Payment p = payment(order(), "initial");
        p.setInitiationState("UNKNOWN");
        p.setLeaseToken("lost-worker");
        p.setLeaseUntil(OffsetDateTime.now().minusMinutes(2));
        p.setNextQueryAt(OffsetDateTime.now().minusMinutes(1));
        p.setClientIp("127.0.0.1");
        payments.saveAndFlush(p);
        org.mockito.Mockito.when(gateway.query(org.mockito.ArgumentMatchers.any()))
                .thenReturn(success(p, "200"));
        service.reconcile(p.getId());
        assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        org.mockito.Mockito.verify(gateway, org.mockito.Mockito.never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void staleOsivEntityCannotOverwriteCommittedCallback() throws Exception {
        Payment p = payment(order(), "initial");
        var em = entityManagerFactory.createEntityManager();
        org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(
                entityManagerFactory, new org.springframework.orm.jpa.EntityManagerHolder(em));
        try {
            // Emulate OSIV caching the attempt before an independent webhook commits.
            em.find(Payment.class, p.getId());
            try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> service.apply(p.getProvider(), success(p, "201")))
                        .get(15, TimeUnit.SECONDS);
            }
            assertThat(service.apply(p.getProvider(), success(p, "201")))
                    .isEqualTo(OnlinePaymentService.Applied.DUPLICATE);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(
                    entityManagerFactory);
            em.close();
        }
        assertThat(histories.findByOrderIdOrdered(p.getOrder().getId())).hasSize(1);
    }

    private void race(Callable<?> a, Callable<?> b) throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Future<?> one = executor.submit(() -> {
                start.await();
                return a.call();
            });
            Future<?> two = executor.submit(() -> {
                start.await();
                return b.call();
            });
            start.countDown();
            one.get(15, TimeUnit.SECONDS);
            two.get(15, TimeUnit.SECONDS);
        }
    }

    private PaymentGateway.Result success(Payment p, String reference) {
        return new PaymentGateway.Result(
                p.getMerchantRequestId(), p.getAmount(), "VND", PaymentGateway.Outcome.SUCCEEDED, reference, null);
    }

    private Payment payment(Order order, String key) {
        return payments.saveAndFlush(Payment.builder()
                .order(order)
                .provider(PaymentProvider.MOMO)
                .method(PaymentMethod.ONLINE)
                .status(PaymentAttemptStatus.PENDING)
                .amount(order.getTotalAmount())
                .currency("VND")
                .idempotencyKey(key)
                .merchantRequestId(UUID.randomUUID().toString())
                .paymentDeadline(OffsetDateTime.now().plusMinutes(15))
                .initiationState("READY")
                .applicationStatus("NONE")
                .build());
    }

    private Order order() {
        return orders.saveAndFlush(Order.builder()
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 12))
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(10000))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(10000))
                .currency("VND")
                .idempotencyKey(UUID.randomUUID().toString())
                .requestHash("b".repeat(64))
                .recipientName("Test")
                .phone("0900000000")
                .fullAddress("Test")
                .build());
    }
}
