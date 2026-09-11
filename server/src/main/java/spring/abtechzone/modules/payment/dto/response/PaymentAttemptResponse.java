package spring.abtechzone.modules.payment.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentAttemptResponse {
    Long id;
    String provider;
    String method;
    String status;
    BigDecimal amount;
    String currency;
    String providerReference;
    OffsetDateTime paidAt;
    OffsetDateTime createdAt;
    OffsetDateTime paymentDeadline;
    String initiationState;
    String applicationStatus;
    String reviewReason;
}
