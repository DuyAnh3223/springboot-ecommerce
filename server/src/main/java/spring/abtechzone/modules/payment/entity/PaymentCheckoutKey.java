package spring.abtechzone.modules.payment.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.*;

/** Idempotency receipt, including keys that reuse the initial or active attempt. */
@Entity
@Table(
        name = "payment_checkout_key",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_payment_checkout_key",
                        columnNames = {"order_id", "request_key"}))
@Getter
@Setter
@NoArgsConstructor
public class PaymentCheckoutKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "request_key", nullable = false, length = 36)
    private String requestKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Payment payment;
}
