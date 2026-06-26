package spring.abtechzone.modules.voucher.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank
    String name;

    String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    VoucherType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal value;

    @NotBlank
    @Column(nullable = false, unique = true)
    String code; // voucher code

    LocalDateTime startDate;
    LocalDateTime endDate;

    Integer maxUses; // Số lượng voucher được áp dụng
    Integer usedCount; // Số voucher đã sử dụng
    Integer maxPerUser; // Số voucher tối đa 1 user dc dùng

    @Column(precision = 12, scale = 2)
    BigDecimal minOrderValue; // Giá trị đơn hàng tối thiểu

    boolean isActive;

    @ManyToMany
    @JoinTable(
            name = "voucher_users",
            joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    List<User> userIds = new ArrayList<>(); // Users nào đã sử dụng

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    VoucherApplyScope applyScope;

    @ManyToMany
    @JoinTable(
            name = "voucher_product_skus",
            joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "product_sku_id"))
    @Builder.Default
    Set<ProductSku> productSkus = new HashSet<>();
}
