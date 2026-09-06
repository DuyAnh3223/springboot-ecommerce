package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.constant.PaymentStatus;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.payment.config.PaymentMockPolicy;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.dto.PaymentSummary;
import spring.abtechzone.modules.payment.dto.request.MockPaymentResultRequest;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.mapper.PaymentMapper;
import spring.abtechzone.modules.payment.service.OrderPaymentService;
import spring.abtechzone.modules.payment.service.PaymentService;
import spring.abtechzone.modules.user.entity.User;

@ExtendWith(MockitoExtension.class)
class OrderPaymentServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderStatusHistoryRepository historyRepository;

    @Mock
    PaymentService paymentService;

    @Mock
    PaymentMockPolicy mockPolicy;

    @Mock
    TransactionTemplate transactionTemplate;

    @Spy
    PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);

    @InjectMocks
    OrderPaymentService orderPaymentService;

    private Order order;
    private Payment payment;
    private User admin;
    private MockPaymentResultRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        order = Order.builder()
                .id(10L)
                .orderCode("ORD-1")
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();
        payment = Payment.builder()
                .id(20L)
                .order(order)
                .provider(PaymentProvider.MOCK)
                .method(PaymentMethod.MOCK)
                .status(PaymentAttemptStatus.PENDING)
                .amount(new BigDecimal("100000.00"))
                .currency("VND")
                .build();
        admin = User.builder().id(UUID.randomUUID()).build();
        request = MockPaymentResultRequest.builder()
                .outcome(PaymentAttemptStatus.SUCCEEDED)
                .providerReference("TX-1")
                .amount(new BigDecimal("100000"))
                .currency("VND")
                .build();
    }

    @Test
    void success_confirmsOrderAndWritesHistoryOnce() {
        stubPaymentLoad();
        when(paymentService.applyMockResult(any(), any(), any(), any(), any())).thenReturn(true);
        when(paymentService.getSummary(order)).thenReturn(new PaymentSummary(PaymentMethod.MOCK, PaymentStatus.PAID));

        var response = orderPaymentService.applyMockResult(20L, request, admin);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getPaymentStatus()).isEqualTo("PAID");
        verify(historyRepository).save(any());
        verify(orderRepository).flush();
    }

    @Test
    void exactReplay_doesNotWriteSecondHistory() {
        payment.setStatus(PaymentAttemptStatus.SUCCEEDED);
        order.setStatus(OrderStatus.CONFIRMED);
        stubPaymentLoad();
        when(paymentService.applyMockResult(any(), any(), any(), any(), any())).thenReturn(false);
        when(paymentService.getSummary(order)).thenReturn(new PaymentSummary(PaymentMethod.MOCK, PaymentStatus.PAID));

        orderPaymentService.applyMockResult(20L, request, admin);

        verify(historyRepository, never()).save(any());
    }

    @Test
    void lateSuccessAfterCancellation_isRejectedBeforePaymentMutation() {
        order.setStatus(OrderStatus.CANCELLED);
        stubPaymentLoad();

        assertThatThrownBy(() -> orderPaymentService.applyMockResult(20L, request, admin))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PAYMENT_STATE_CONFLICT.getMessage());
        verify(paymentService, never()).applyMockResult(any(), any(), any(), any(), any());
    }

    @Test
    void providerReferenceConstraintFailure_isTranslatedAfterTransactionRollback() {
        when(paymentService.getOrderIdForPayment(20L)).thenReturn(10L);
        reset(transactionTemplate);
        when(transactionTemplate.execute(any()))
                .thenThrow(new DataIntegrityViolationException("uk_payment_provider_reference"));

        assertThatThrownBy(() -> orderPaymentService.applyMockResult(20L, request, admin))
                .isInstanceOf(AppException.class)
                .hasMessage(ErrorCode.PAYMENT_REFERENCE_CONFLICT.getMessage());
    }

    private void stubPaymentLoad() {
        when(paymentService.getOrderIdForPayment(20L)).thenReturn(10L);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(paymentService.getPayment(20L, order)).thenReturn(payment);
    }
}
