package spring.abtechzone.modules.voucher.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;

import lombok.Getter;
import lombok.Setter;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;

@Getter
@Setter
@Entity
@Table(
        name = "voucher_redemption",
        indexes = {
            @Index(name = "idx_voucher_redemption_voucher_status", columnList = "voucher_id,status"),
            @Index(name = "idx_voucher_redemption_voucher_user_status", columnList = "voucher_id,user_id,status")
        },
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_voucher_redemption_order",
                        columnNames = {"order_id"}))
public class VoucherRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoucherRedemptionStatus status;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
