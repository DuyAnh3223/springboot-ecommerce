package spring.abtechzone.modules.order.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.constant.PaymentStatus;
import spring.abtechzone.modules.voucher.entity.Voucher;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "\"order\"",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_order_user_idempotency_key",
                        columnNames = {"user_id", "idempotency_key"}))
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Version
    @Column(name = "version")
    Long version;

    @NotNull
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "seller_id")
    UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @NotNull
    @Column(name = "subtotal_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal subtotalAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal discountAmount;

    @NotNull
    @Column(name = "shipping_fee", nullable = false, precision = 14, scale = 2)
    BigDecimal shippingFee;

    @NotNull
    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal totalAmount;

    @Size(max = 3)
    @NotNull
    @ColumnDefault("'VND'")
    @Column(name = "currency", nullable = false, length = 3)
    String currency;

    @Size(max = 50)
    @Column(name = "voucher_code", length = 50)
    String voucherCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    Voucher voucher;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    PaymentStatus paymentStatus;

    @NotNull
    @Column(name = "idempotency_key", nullable = false, length = 36)
    String idempotencyKey;

    @NotNull
    @Column(name = "request_hash", nullable = false, length = 64)
    String requestHash;

    @Column(name = "shipping_address_id")
    UUID shippingAddressId;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    String note;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", nullable = false)
    OffsetDateTime createdAt;

    @Column(nullable = false, unique = true)
    String orderCode;

    // Snapshot shipping address
    @Column(nullable = false)
    String recipientName;

    @Column(nullable = false)
    String phone;

    @Column(nullable = false)
    String fullAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<OrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (currency == null) {
            currency = "VND";
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
