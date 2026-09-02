package spring.abtechzone.modules.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Check;
import org.springframework.data.domain.Persistable;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.product.entity.ProductSku;

@Entity
@Table(name = "inventory")
@Check(constraints = "on_hand >= 0")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory implements Persistable<Long> {

    @Id
    @Column(name = "sku_id", nullable = false)
    Long skuId;

    /**
     * Inventory uses a shared, manually assigned primary key. Tell Spring Data
     * to persist a newly-created row instead of treating the SKU id as a
     * detached entity id and calling merge().
     */
    @Transient
    @Builder.Default
    boolean newEntity = true;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false, unique = true)
    ProductSku productSku;

    @NotNull
    @Min(0)
    @Column(name = "on_hand", nullable = false)
    Integer onHand;

    @Override
    public Long getId() {
        return skuId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }
}
