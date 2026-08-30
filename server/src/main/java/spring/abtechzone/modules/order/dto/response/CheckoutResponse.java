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
    BigDecimal eligibleSubtotal;
    BigDecimal shippingFee;
    BigDecimal discountAmount;
    BigDecimal totalAmount;
    VoucherReviewResponse voucher;
    boolean canPlaceOrder;
}
