package spring.abtechzone.modules.inventory.entity;

import java.util.List;

import jakarta.persistence.*;

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
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Integer stock;
    List<Long> reservation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_sku_id", nullable = false, unique = true)
    ProductSku productSku;
}
