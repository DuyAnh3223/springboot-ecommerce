package spring.abtechzone.modules.payment.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.payment.constant.PaymentAttemptStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MockPaymentResultRequest {
    @NotBlank
    @Size(max = 150)
    String providerReference;

    @NotNull
    PaymentAttemptStatus outcome;

    @NotNull
    @DecimalMin("0.00")
    BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "[A-Z]{3}")
    String currency;
}
