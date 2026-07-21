package spring.abtechzone.modules.user.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressResponse {
    UUID userId;
    String recipientName;
    String phone;
    String province;
    String district;
    String ward;
    String streetAddress;
    String country;
    boolean isDefault;
}
