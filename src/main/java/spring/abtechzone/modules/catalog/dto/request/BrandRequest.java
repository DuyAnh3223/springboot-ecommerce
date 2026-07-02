package spring.abtechzone.modules.catalog.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrandRequest {
    String name;
    String slug;
    String logoUrl;
}
