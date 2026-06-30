package spring.abtechzone.modules.order.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutItemResponse {
    Long productSkuId;
    String productName;
    String skuCode;
    String imageUrl;
    int quantity;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
}
