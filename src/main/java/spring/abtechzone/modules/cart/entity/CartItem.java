package spring.abtechzone.modules.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;


    Integer quantity;
    BigDecimal unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    ProductSku productSku;
}
