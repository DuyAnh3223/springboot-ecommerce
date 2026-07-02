package spring.abtechzone.modules.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {
    java.util.UUID addressId;

    @Valid
    AddressRequest newUserAddress;

    String voucherCode;

    @NotBlank(message = "Payment method is required")
    String paymentMethod;
}
