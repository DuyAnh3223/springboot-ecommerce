package spring.abtechzone.modules.catalog.dto.response;

import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

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

    List<CategoryResponse> categories;
    List<BrandResponse> brands;

    Map<String, Object> attributes;
    List<ProductSkuResponse> productSkus;
}
