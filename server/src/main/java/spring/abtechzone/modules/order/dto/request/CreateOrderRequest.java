package spring.abtechzone.modules.order.dto.request;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.order.constant.PaymentMethod;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {
    @NotNull(message = "reviewedCheckout is required")
    @Valid
    ReviewedCheckoutRequest reviewedCheckout;

    UUID addressId;

    @Valid
    AddressRequest newUserAddress;

    @NotNull(message = "paymentMethod is required")
    PaymentMethod paymentMethod;
}
