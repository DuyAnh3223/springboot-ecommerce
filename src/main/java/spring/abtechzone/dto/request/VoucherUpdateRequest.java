package spring.abtechzone.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.constant.VoucherApplyScope;
import spring.abtechzone.constant.VoucherType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherUpdateRequest {

    @NotBlank(message = "VOUCHER_NAME_NOT_BLANK")
    String name;

    String description;

    @NotNull(message = "VOUCHER_TYPE_NOT_NULL")
    VoucherType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "VALUE_INVALID")
    BigDecimal value;

    @NotBlank(message = "VOUCHER_CODE_NOT_BLANK")
    String code;

    @NotNull(message = "START_DATE_NOT_NULL")
    LocalDateTime startDate;

    @NotNull(message = "END_DATE_NOT_NULL")
    LocalDateTime endDate;

    @PositiveOrZero(message = "VOUCHER_QUANTITY_INVALID")
    Integer maxUses;

    @PositiveOrZero(message = "VOUCHER_PER_USER_INVALID")
    Integer maxPerUser;

    @PositiveOrZero(message = "VOUCHER_MIN_ORDER_VALUE_INVALID")
    BigDecimal minOrderValue;

    boolean isActive;

    @NotNull(message = "SCOPE_NOT_NULL")
    VoucherApplyScope applyScope;

    List<Long> productSkuIds;
}
