package spring.abtechzone.modules.product.dto.response;

import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.category.dto.response.BrandResponse;
import spring.abtechzone.modules.category.dto.response.CategoryResponse;

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

    CategoryResponse category;
    BrandResponse brand;

    Map<String, Object> attributes;
    List<ProductSkuResponse> productSkus;
}
