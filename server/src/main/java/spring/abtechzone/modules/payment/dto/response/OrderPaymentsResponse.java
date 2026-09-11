package spring.abtechzone.modules.payment.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderPaymentsResponse {
    String orderCode;
    String orderStatus;
    String paymentMethod;
    String paymentStatus;
    boolean retryAllowed;
    List<PaymentAttemptResponse> attempts;
}
