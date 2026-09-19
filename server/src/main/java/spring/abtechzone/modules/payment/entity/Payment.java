package spring.abtechzone.modules.payment.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;
import spring.abtechzone.modules.payment.constant.PaymentProvider;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Check(
        constraints = "amount >= 0 and (provider_reference is null or btrim(provider_reference) <> '') "
                + "and ((status = 'SUCCEEDED' and paid_at is not null) "
                + "or (status <> 'SUCCEEDED' and paid_at is null)) "
                + "and (applied_order_id is null or (applied_order_id = order_id and status = 'SUCCEEDED' "
                + "and application_status is not null and application_status = 'APPLIED'))")
@Table(
        name = "payment",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_payment_order_idempotency_key",
                    columnNames = {"order_id", "idempotency_key"}),
            @UniqueConstraint(
                    name = "uk_payment_provider_reference",
                    columnNames = {"provider", "provider_reference"})
        },
        indexes = {
            @Index(name = "idx_payment_order_created", columnList = "order_id, created_at, id"),
            @Index(name = "idx_payment_next_query", columnList = "next_query_at, id"),
            @Index(name = "idx_payment_deadline", columnList = "payment_deadline, id")
        })
public class Payment {

    @Column(name = "merchant_request_id", unique = true, length = 50)
    String merchantRequestId;

    @Column(name = "provider_checkout_id", length = 150)
    String providerCheckoutId;

    @Column(name = "checkout_url", length = 2048)
    String checkoutUrl;

    @Column(name = "payment_deadline")
    OffsetDateTime paymentDeadline;

    @Column(name = "initiation_state", length = 20)
    String initiationState;

    @Column(name = "application_status", length = 20)
    String applicationStatus;

    @Column(name = "review_reason", length = 100)
    String reviewReason;

    // Nullable unique key enforces at most one applied payment per order in PostgreSQL.
    @Column(name = "applied_order_id", unique = true)
    Long appliedOrderId;

    @Column(name = "next_query_at")
    OffsetDateTime nextQueryAt;

    @Column(name = "query_count")
    Integer queryCount;

    @Column(name = "lease_until")
    OffsetDateTime leaseUntil;

    @Column(name = "lease_token", length = 36)
    String leaseToken;

    @Column(name = "client_ip", length = 45)
    String clientIp;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentProvider provider;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentMethod method;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    PaymentAttemptStatus status;

    @NotNull
    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal amount;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    String currency;

    @Size(max = 150)
    @Column(name = "provider_reference", length = 150)
    String providerReference;

    @Column(name = "raw_payload", columnDefinition = "text")
    String rawPayload;

    @Column(name = "paid_at")
    OffsetDateTime paidAt;

    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "idempotency_key", nullable = false, length = 36)
    String idempotencyKey;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", nullable = false)
    OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
