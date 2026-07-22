package spring.abtechzone.modules.user.dto.request;

import java.util.UUID;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
    String ward;
    String street;
    String country;
    Boolean isDefault;
}
