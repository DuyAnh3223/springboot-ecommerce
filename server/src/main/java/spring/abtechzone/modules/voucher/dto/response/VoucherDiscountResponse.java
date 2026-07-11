package spring.abtechzone.modules.voucher.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherDiscountResponse {
    BigDecimal discountAmount;
    BigDecimal totalOrder;
    BigDecimal totalPrice;
}
