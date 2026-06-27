package spring.abtechzone.modules.cart.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemRespone {
    Integer quantity;
    BigDecimal unitPrice;
    Long cartId;
    String productName;
}
