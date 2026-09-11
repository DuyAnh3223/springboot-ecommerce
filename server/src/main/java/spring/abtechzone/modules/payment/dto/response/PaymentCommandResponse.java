package spring.abtechzone.modules.payment.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentCommandResponse {
    PaymentAttemptResponse payment;
    String orderCode;
    String orderStatus;
    String paymentStatus;
}
