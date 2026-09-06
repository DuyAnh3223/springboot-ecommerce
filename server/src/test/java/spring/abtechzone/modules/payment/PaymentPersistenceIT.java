package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.entity.Payment;
import spring.abtechzone.modules.payment.repository.PaymentRepository;

class PaymentPersistenceIT extends BaseIT {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void orderHasManyAttemptsAndNullableReferences() {
        Order order = saveOrder("ORD-PAY-1");

        paymentRepository.saveAndFlush(payment(order, "initial", null));
        paymentRepository.saveAndFlush(payment(order, UUID.randomUUID().toString(), null));

        assertThat(paymentRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId()))
                .hasSize(2);
    }

    @Test
    void providerReferenceIsUniqueAcrossOrders() {
        Order first = saveOrder("ORD-PAY-2");
        Order second = saveOrder("ORD-PAY-3");
        paymentRepository.saveAndFlush(payment(first, "initial", "TX-1"));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment(second, "initial", "TX-1")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void idempotencyKeyIsUniqueWithinAnOrder() {
        Order order = saveOrder("ORD-PAY-4");
        paymentRepository.saveAndFlush(payment(order, "initial", null));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(payment(order, "initial", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cleanSchemaHasPaymentOwnershipColumnsOnly() {
        Integer paymentColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'payment' "
                        + "and column_name in ('provider', 'method', 'status', 'provider_reference', 'idempotency_key')",
                Integer.class);
        Integer obsoleteOrderColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'order' "
                        + "and column_name in ('payment_method', 'payment_status')",
                Integer.class);

        assertThat(paymentColumns).isEqualTo(5);
        assertThat(obsoleteOrderColumns).isZero();
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
                .requestHash("a".repeat(64))
                .recipientName("Test User")
                .phone("0900000000")
                .fullAddress("1 Test Street")
                .build());
    }

    private Payment payment(Order order, String key, String reference) {
        return Payment.builder()
                .order(order)
                .provider(PaymentProvider.MOCK)
                .method(PaymentMethod.MOCK)
                .status(PaymentAttemptStatus.PENDING)
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .providerReference(reference)
                .idempotencyKey(key)
                .build();
    }
}
