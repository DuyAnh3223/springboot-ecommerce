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
    UUID id;
    UUID userId;
    String recipientName;
    String phone;
    String province;
    String district;
    String ward;
    Integer ghnProvinceId;
    Integer ghnDistrictId;
    String ghnWardCode;
    String street;
    String country;
    Boolean isDefault;
}
