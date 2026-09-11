package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.constant.PaymentStatus;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.repository.PaymentRepository;
import spring.abtechzone.modules.payment.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    PaymentService paymentService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(10L)
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100000.00"))
                .currency("VND")
                .build();
    }

    @Test
    void createInitialPayment_usesServerOwnedOrderMoney() {
        when(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "initial")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createInitialPayment(order, PaymentMethod.COD);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("100000.00");
        assertThat(saved.getCurrency()).isEqualTo("VND");
        assertThat(saved.getProvider()).isEqualTo(PaymentProvider.INTERNAL);
        assertThat(saved.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
    }

    @Test
    void applyMockSuccess_exactReplayHasNoSecondSideEffect() {
        Payment payment = mockPayment(PaymentAttemptStatus.PENDING);
        when(paymentRepository.findByProviderAndProviderReference(PaymentProvider.MOCK, "TX-1"))
                .thenReturn(Optional.empty(), Optional.of(payment));

        assertThat(paymentService.applyMockResult(
                        payment, PaymentAttemptStatus.SUCCEEDED, " TX-1 ", new BigDecimal("100000"), "VND"))
                .isTrue();
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(paymentService.applyMockResult(
                        payment, PaymentAttemptStatus.SUCCEEDED, "TX-1", new BigDecimal("100000.00"), "VND"))
                .isFalse();
    }

    @Test
    void applyMockResult_rejectsMoneyMismatchBeforeMutation() {
        Payment payment = mockPayment(PaymentAttemptStatus.PENDING);

        assertThatThrownBy(() -> paymentService.applyMockResult(
                        payment, PaymentAttemptStatus.SUCCEEDED, "TX-1", new BigDecimal("1"), "VND"))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());
        assertThat(payment.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
        verify(paymentRepository, never()).findByProviderAndProviderReference(any(), any());
    }

    @Test
    void applyMockResult_rejectsConflictingTerminalOutcome() {
        Payment payment = mockPayment(PaymentAttemptStatus.FAILED);
        payment.setProviderReference("TX-1");
        when(paymentRepository.findByProviderAndProviderReference(PaymentProvider.MOCK, "TX-1"))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.applyMockResult(
                        payment, PaymentAttemptStatus.SUCCEEDED, "TX-1", new BigDecimal("100000"), "VND"))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PAYMENT_STATE_CONFLICT.getMessage());
        assertThat(payment.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void applyMockResult_rejectsReferenceOwnedByAnotherAttempt() {
        Payment payment = mockPayment(PaymentAttemptStatus.PENDING);
        Payment owner = mockPayment(PaymentAttemptStatus.SUCCEEDED);
        owner.setId(21L);
        owner.setProviderReference("TX-1");
        when(paymentRepository.findByProviderAndProviderReference(PaymentProvider.MOCK, "TX-1"))
                .thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> paymentService.applyMockResult(
                        payment, PaymentAttemptStatus.SUCCEEDED, "TX-1", new BigDecimal("100000"), "VND"))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PAYMENT_REFERENCE_CONFLICT.getMessage());
        assertThat(payment.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
    }

    @Test
    void retryMock_replaysSameKeyAndRejectsAnotherPendingAttempt() {
        Payment pending = mockPayment(PaymentAttemptStatus.PENDING);
        pending.setIdempotencyKey("existing-key");
        when(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "existing-key"))
                .thenReturn(Optional.of(pending));

        assertThat(paymentService.retryMockPayment(order, "existing-key")).isSameAs(pending);

        when(paymentRepository.findByOrderIdAndIdempotencyKey(10L, "new-key")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderIdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of(pending));
        assertThatThrownBy(() -> paymentService.retryMockPayment(order, "new-key"))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PAYMENT_STATE_CONFLICT.getMessage());
    }

    @Test
    void summary_usesPaymentAsSourceOfTruth() {
        Payment failed = mockPayment(PaymentAttemptStatus.FAILED);
        Payment succeeded = mockPayment(PaymentAttemptStatus.SUCCEEDED);
        when(paymentRepository.findByOrderIdInOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of(failed, succeeded));

        assertThat(paymentService.getSummary(order).status()).isEqualTo(PaymentStatus.PAID);
    }

    private Payment mockPayment(PaymentAttemptStatus status) {
        return Payment.builder()
                .id(20L)
                .order(order)
                .provider(PaymentProvider.MOCK)
                .method(PaymentMethod.MOCK)
                .status(status)
                .amount(new BigDecimal("100000.00"))
                .currency("VND")
                .idempotencyKey("initial")
                .build();
    }
}
