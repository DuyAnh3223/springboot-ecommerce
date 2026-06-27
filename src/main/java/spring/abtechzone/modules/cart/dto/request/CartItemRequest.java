package spring.abtechzone.modules.cart.dto.request;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.product.entity.Product;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemRequest {
    Integer quantity;
    BigDecimal unitPrice;
    Long cartId;
    Long productSkuId;
}
