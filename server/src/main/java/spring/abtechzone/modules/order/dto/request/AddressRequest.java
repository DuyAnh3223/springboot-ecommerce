package spring.abtechzone.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressRequest {
    @NotBlank(message = "Recipient name is required")
    String recipientName;

    @NotBlank(message = "Phone is required")
    String phone;

    @NotBlank(message = "Province is required")
    String province;

    @NotBlank(message = "Ward is required")
    String ward;

    @NotBlank(message = "Street is required")
    String street;

    boolean saveAddress;
}
