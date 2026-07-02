package spring.abtechzone.modules.catalog.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.catalog.entity.ProductAttribute;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {

    Long id;
    String name;
    String slug;
    String thumbnail;
    String description;

    Double rating;
    boolean isDraft;
    boolean isPublished;

    List<ProductAttribute> attributes;
    List<ProductSkuResponse> productSkus;
}
