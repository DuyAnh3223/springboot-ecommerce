package spring.abtechzone.modules.order.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.entity.Voucher;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    String paymentMethod;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal shippingFee;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal totalDiscount;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal totalCheckout;

    // Snapshot shipping address
    @Column(nullable = false)
    String recipientName;

    @Column(nullable = false)
    String phone;

    @Column(nullable = false)
    String fullAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<OrderItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    Voucher voucher;
}
