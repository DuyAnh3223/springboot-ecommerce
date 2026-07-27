package spring.abtechzone.modules.product.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImageResponse {

    Long id;
    String url;
    String accessUrl;
    Integer sortOrder;

    Boolean primary;

}
