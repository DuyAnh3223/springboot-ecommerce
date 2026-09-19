package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;

import spring.abtechzone.modules.order.constant.*;
import spring.abtechzone.modules.order.entity.*;
import spring.abtechzone.modules.order.repository.*;
import spring.abtechzone.modules.order.service.OrderLifecycleService;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.constant.*;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.gateway.*;
import spring.abtechzone.modules.payment.repository.PaymentRepository;
import spring.abtechzone.modules.payment.service.OnlinePaymentService;

class OnlinePaymentServiceTest {
    PaymentRepository payments = mock(PaymentRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    OrderStatusHistoryRepository histories = mock(OrderStatusHistoryRepository.class);
    OrderLifecycleService lifecycle = mock(OrderLifecycleService.class);
    GatewayRegistry gateways = mock(GatewayRegistry.class);
    OffsetDateTime now = OffsetDateTime.parse("2026-09-06T08:00:00Z");
    OnlinePaymentService service;
    Payment p;
    Order order;

    @BeforeEach
    void setup() {
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new OnlinePaymentService(
                payments,
                orders,
                histories,
                lifecycle,
                gateways,
                new OnlinePaymentProperties(),
                new TransactionTemplate(manager),
                Clock.fixed(now.toInstant(), ZoneOffset.UTC),
                mock(jakarta.persistence.EntityManager.class),
                mock(spring.abtechzone.modules.payment.repository.PaymentCheckoutKeyRepository.class));
        order = new Order();
        order.setId(1L);
        order.setOrderCode("ORD1");
        order.setStatus(OrderStatus.PENDING);
        order.setUserId(UUID.randomUUID());
        p = Payment.builder()
                .id(2L)
                .order(order)
                .provider(PaymentProvider.MOMO)
                .method(PaymentMethod.ONLINE)
                .status(PaymentAttemptStatus.PENDING)
                .amount(BigDecimal.valueOf(10000))
                .currency("VND")
                .idempotencyKey("initial")
                .merchantRequestId("2")
                .paymentDeadline(now.plusMinutes(15))
                .createdAt(now)
                .initiationState("NEW")
                .applicationStatus("NONE")
                .build();
        when(payments.findIdByMerchant(PaymentProvider.MOMO, "2")).thenReturn(Optional.of(2L));
        when(payments.findOrderIdByPaymentId(2L)).thenReturn(Optional.of(1L));
        when(orders.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orders.findByOrderCodeForUpdate("ORD1")).thenReturn(Optional.of(order));
        when(payments.findById(2L)).thenReturn(Optional.of(p));
        when(payments.findByOrderIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(p));
    }

    PaymentGateway.Result success() {
        return new PaymentGateway.Result("2", p.getAmount(), "VND", PaymentGateway.Outcome.SUCCEEDED, "12345", null);
    }

    @Test
    void duplicateSuccessConfirmsOnlyOnceAndFailureCannotDowngrade() {
        assertThat(service.apply(PaymentProvider.MOMO, success())).isEqualTo(OnlinePaymentService.Applied.RECORDED);
        assertThat(service.apply(PaymentProvider.MOMO, success())).isEqualTo(OnlinePaymentService.Applied.DUPLICATE);
        service.apply(
                PaymentProvider.MOMO,
                new PaymentGateway.Result("2", p.getAmount(), "VND", PaymentGateway.Outcome.FAILED, null, null));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(p.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(p.getAppliedOrderId()).isEqualTo(1L);
        verify(histories, times(1)).save(any());
        verifyNoInteractions(lifecycle);
    }

    @Test
    void lateSuccessRetainsMoneyWithoutReopeningCancelledOrder() {
        order.setStatus(OrderStatus.CANCELLED);
        p.setStatus(PaymentAttemptStatus.CANCELLED);
        service.apply(PaymentProvider.MOMO, success());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(p.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(p.getApplicationStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(p.getPaidAt()).isEqualTo(now);
        verifyNoInteractions(histories);
    }

    @Test
    void deadlineIsCheckedWithoutWaitingForJob() {
        p.setPaymentDeadline(now);
        service.apply(PaymentProvider.MOMO, success());
        verify(lifecycle).expireOnlineOrder(order);
        assertThat(p.getReviewReason()).isEqualTo("LATE_SUCCESS");
        verifyNoInteractions(histories);
    }

    @Test
    void amountMismatchIsVisibleAndDoesNotConfirm() {
        assertThat(service.apply(
                        PaymentProvider.MOMO,
                        new PaymentGateway.Result(
                                "2", BigDecimal.ONE, "VND", PaymentGateway.Outcome.SUCCEEDED, "12345", null)))
                .isEqualTo(OnlinePaymentService.Applied.MISMATCH);
        assertThat(p.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
        assertThat(p.getApplicationStatus()).isEqualTo("REVIEW_REQUIRED");
        verifyNoInteractions(histories);
    }

    @Test
    void referenceOwnedByAnotherAttemptIsRejected() {
        when(payments.findByProviderAndProviderReference(PaymentProvider.MOMO, "12345"))
                .thenReturn(Optional.of(Payment.builder().id(5L).build()));
        assertThat(service.apply(PaymentProvider.MOMO, success())).isEqualTo(OnlinePaymentService.Applied.CONFLICT);
        verifyNoInteractions(histories);
    }

    @Test
    void extraChargeRetainsFinancialSuccessForReview() {
        var other = Payment.builder().id(9L).appliedOrderId(1L).build();
        when(payments.findByOrderIdOrderByCreatedAtAscIdAsc(1L)).thenReturn(List.of(p, other));
        service.apply(PaymentProvider.MOMO, success());
        assertThat(p.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(p.getReviewReason()).isEqualTo("EXTRA_PAYMENT");
        verifyNoInteractions(histories);
    }

    @Test
    void networkTimeoutNeverFailsOrCreatesAnotherAttempt() {
        var gateway = mock(PaymentGateway.class);
        when(gateways.require(PaymentProvider.MOMO)).thenReturn(gateway);
        when(gateway.create(any())).thenThrow(new GatewayException(GatewayException.Kind.UNAVAILABLE));
        service.checkout("ORD1", UUID.randomUUID().toString(), PaymentProvider.MOMO, order.getUserId(), "127.0.0.1");
        service.checkout("ORD1", UUID.randomUUID().toString(), PaymentProvider.MOMO, order.getUserId(), "127.0.0.1");
        assertThat(p.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
        assertThat(p.getInitiationState()).isEqualTo("UNKNOWN");
        assertThat(p.getNextQueryAt()).isAfter(now);
        verify(gateway, times(1)).create(any());
        verify(payments, never()).saveAndFlush(any());
    }

    @Test
    void callbackBeforeCreateResponseWins() {
        var gateway = mock(PaymentGateway.class);
        when(gateways.require(PaymentProvider.MOMO)).thenReturn(gateway);
        when(gateway.create(any())).thenAnswer(invocation -> {
            service.apply(PaymentProvider.MOMO, success());
            return new PaymentGateway.Checkout("https://test-payment.momo.vn/pay", "2");
        });
        var result = service.checkout(
                "ORD1", UUID.randomUUID().toString(), PaymentProvider.MOMO, order.getUserId(), "127.0.0.1");
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.checkoutUrl()).isNull();
        assertThat(p.getInitiationState()).isEqualTo("RESOLVED");
    }

    @Test
    void anotherOwnerCannotInitiateCheckout() {
        assertThatThrownBy(() -> service.checkout(
                        "ORD1", UUID.randomUUID().toString(), PaymentProvider.MOMO, UUID.randomUUID(), "127.0.0.1"))
                .isInstanceOf(spring.abtechzone.common.exception.AppException.class);
        verify(payments, never()).saveAndFlush(any());
    }
}
