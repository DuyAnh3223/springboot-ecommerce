package spring.abtechzone.entity;

import java.util.List;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
