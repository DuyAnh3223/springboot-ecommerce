package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.dto.request.MockPaymentResultRequest;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.repository.PaymentRepository;
import spring.abtechzone.modules.payment.service.OrderPaymentService;
import spring.abtechzone.modules.user.entity.User;

@TestPropertySource(properties = "app.payment.mock-enabled=true")
class PaymentWorkflowIT extends BaseIT {

    @Autowired
    OrderPaymentService orderPaymentService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderStatusHistoryRepository historyRepository;

    private ExecutorService executor;
    private User admin;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        historyRepository.deleteAll();
        orderRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
        admin = User.builder().id(UUID.randomUUID()).build();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentDuplicateSuccessAppliesOneOrderTransition() throws Exception {
        Order order = saveOrder("ORD-RACE-SUCCESS");
        Payment payment = paymentRepository.saveAndFlush(payment(order, PaymentAttemptStatus.PENDING, "initial"));
        MockPaymentResultRequest request = result(PaymentAttemptStatus.SUCCEEDED, "TX-RACE-1");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Object>> futures = List.of(
                executor.submit(() -> invokeResult(ready, start, payment.getId(), request)),
                executor.submit(() -> invokeResult(ready, start, payment.getId(), request)));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        for (Future<Object> future : futures) {
            assertThat(future.get(10, TimeUnit.SECONDS)).isNotInstanceOf(Throwable.class);
        }
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(historyRepository.findByOrderIdOrdered(order.getId())).hasSize(1);
    }

    @Test
    void concurrentDifferentRetryKeysCreateAtMostOnePendingAttempt() throws Exception {
        Order order = saveOrder("ORD-RACE-RETRY");
        paymentRepository.saveAndFlush(payment(order, PaymentAttemptStatus.FAILED, "initial"));
        User owner = User.builder().id(order.getUserId()).build();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Object>> futures = List.of(
                executor.submit(() -> invokeRetry(
                        ready, start, order.getOrderCode(), UUID.randomUUID().toString(), owner)),
                executor.submit(() -> invokeRetry(
                        ready, start, order.getOrderCode(), UUID.randomUUID().toString(), owner)));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        long errors = 0;
        for (Future<Object> future : futures) {
            if (future.get(10, TimeUnit.SECONDS) instanceof AppException) {
                errors++;
            }
        }
        assertThat(errors).isEqualTo(1);
        assertThat(paymentRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId()))
                .filteredOn(attempt -> attempt.getStatus() == PaymentAttemptStatus.PENDING)
                .hasSize(1);
    }

    private Object invokeResult(
            CountDownLatch ready, CountDownLatch start, Long paymentId, MockPaymentResultRequest request) {
        try {
            ready.countDown();
            start.await();
            return orderPaymentService.applyMockResult(paymentId, request, admin);
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private Object invokeRetry(CountDownLatch ready, CountDownLatch start, String code, String key, User owner) {
        try {
            ready.countDown();
            start.await();
            return orderPaymentService.retryMock(code, key, owner);
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private Order saveOrder(String code) {
        return orderRepository.saveAndFlush(Order.builder()
                .orderCode(code)
                .userId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .subtotalAmount(new BigDecimal("100000.00"))
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("100000.00"))
                .currency("VND")
                .idempotencyKey(UUID.randomUUID().toString())
                .requestHash("b".repeat(64))
                .recipientName("Test User")
                .phone("0900000000")
                .fullAddress("1 Test Street")
                .build());
    }

    private Payment payment(Order order, PaymentAttemptStatus status, String key) {
        return Payment.builder()
                .order(order)
                .provider(PaymentProvider.MOCK)
                .method(PaymentMethod.MOCK)
                .status(status)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .idempotencyKey(key)
                .build();
    }

    private MockPaymentResultRequest result(PaymentAttemptStatus outcome, String reference) {
        return MockPaymentResultRequest.builder()
                .outcome(outcome)
                .providerReference(reference)
                .amount(new BigDecimal("100000.00"))
                .currency("VND")
                .build();
    }
}
