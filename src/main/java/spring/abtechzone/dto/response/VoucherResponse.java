package spring.abtechzone.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.constant.VoucherApplyScope;
import spring.abtechzone.constant.VoucherType;

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
    List<ProductSkuResponse> productSkus;
}
