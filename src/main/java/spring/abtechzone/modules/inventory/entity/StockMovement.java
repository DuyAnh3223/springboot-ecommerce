package spring.abtechzone.modules.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import spring.abtechzone.modules.catalog.entity.ProductSku;
import spring.abtechzone.modules.user.entity.User;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "stock_movement")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @NotNull
    @Column(name = "change_qty", nullable = false)
    private Integer changeQty;

    @Size(max = 30)
    @NotNull
    @Column(name = "reason", nullable = false, length = 30)
    private String reason;

    @Size(max = 100)
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}