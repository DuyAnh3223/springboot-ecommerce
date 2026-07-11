package spring.abtechzone.modules.order.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    Long orderId;
    String orderCode;
    String orderStatus;
    BigDecimal subtotal;
    BigDecimal shippingFee;
    BigDecimal totalDiscount;
    BigDecimal totalCheckout;
}
