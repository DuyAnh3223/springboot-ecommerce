package spring.abtechzone.modules.order.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.shipment.dto.ShippingAddressSnapshot;

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
    ShippingAddressSnapshot shippingAddress;
    boolean canPlaceOrder;
}
