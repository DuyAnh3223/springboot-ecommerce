package spring.abtechzone.modules.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import spring.abtechzone.modules.catalog.entity.ProductSku;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "product_serial")
public class ProductSerial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    @Size(max = 150)
    @NotNull
    @Column(name = "serial_number", nullable = false, length = 150)
    private String serialNumber;

    @Size(max = 20)
    @NotNull
    @ColumnDefault("'IN_STOCK'")
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Size(max = 100)
    @Column(name = "warehouse_location", length = 100)
    private String warehouseLocation;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "warranty_expire_at")
    private LocalDate warrantyExpireAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}