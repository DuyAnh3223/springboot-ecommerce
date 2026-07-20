package spring.abtechzone.modules.user.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressRequest {
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
