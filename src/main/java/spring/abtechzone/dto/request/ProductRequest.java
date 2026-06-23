package spring.abtechzone.dto.request;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.entity.ProductAttribute;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequest {

    String name;
    String slug;
    String thumbnail;
    String description;

    Boolean isDraft;
    Boolean isPublished;

    List<ProductAttribute> attributes;
    List<ProductSkuRequest> productSkus;
}
