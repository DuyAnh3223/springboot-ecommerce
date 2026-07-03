package spring.abtechzone.modules.order.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.catalog.entity.ProductSku;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    ProductSku sku;

    @Size(max = 255)
    @NotNull
    @Column(name = "product_name_snapshot", nullable = false)
    String productNameSnapshot;

    @Size(max = 100)
    @NotNull
    @Column(name = "sku_snapshot", nullable = false, length = 100)
    String skuSnapshot;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal unitPrice;

    @Column(nullable = false)
    int quantity;

    @Size(max = 150)
    @Column(name = "serial_number", length = 150)
    String serialNumber;

    @Column(name = "warranty_months")
    Integer warrantyMonths;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", nullable = false)
    OffsetDateTime createdAt;

    String imageUrl;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
