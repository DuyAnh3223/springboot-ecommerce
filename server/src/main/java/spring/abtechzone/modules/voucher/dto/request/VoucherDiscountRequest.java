package spring.abtechzone.modules.voucher.dto.request;

import java.math.BigDecimal;
import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherDiscountRequest {
    String code;
    BigDecimal totalOrder;
    Set<Long> productSkuIds;
    Long userId;
}
