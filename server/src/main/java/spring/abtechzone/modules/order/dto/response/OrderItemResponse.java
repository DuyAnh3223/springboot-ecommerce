package spring.abtechzone.modules.order.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {
    Long skuId;
    String skuCode;
    String productName;
    String imageUrl;
    int quantity;
    BigDecimal unitPrice;
    BigDecimal lineTotal;
}
