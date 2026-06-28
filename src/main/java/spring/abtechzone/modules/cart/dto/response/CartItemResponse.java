package spring.abtechzone.modules.cart.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {
    Long productSkuId;
    String skuCode;
    String productName;
    String imageUrl;
    Integer quantity;
    BigDecimal unitPrice;
}
