package spring.abtechzone.modules.voucher.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherResponse {

    String name;
    String description;
    VoucherType type;
    BigDecimal value;
    String code;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer maxUses;
    Integer maxPerUser;
    BigDecimal minOrderValue;
    boolean isActive;
    VoucherApplyScope applyScope;
    Set<ProductSkuResponse> productSkus;
}
