package spring.abtechzone.modules.user.dto.response;

import java.util.UUID;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
