package spring.abtechzone.modules.order.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutResponse {
    List<CheckoutItemResponse> items;
    BigDecimal subtotal;
    BigDecimal shippingFee;
    BigDecimal totalDiscount;
    BigDecimal totalCheckout;
    String voucherCode;
}
