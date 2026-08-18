package spring.abtechzone.modules.order.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherReviewResponse {
    String code;
    boolean applicable;
    String issueCode;
}
