package spring.abtechzone.modules.order.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewedVoucherRequest {
    String code;

    @NotNull(message = "voucher.applicable is required")
    Boolean applicable;
}
