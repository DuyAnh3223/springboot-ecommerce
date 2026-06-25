package spring.abtechzone.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.entity.ProductAttribute;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreateRequest {

    @NotBlank(message = "PRODUCT_NAME_INVALID")
    String name;

    String thumbnail;
    String description;

    Boolean isDraft;
    Boolean isPublished;

    @Valid
    List<ProductAttribute> attributes;

    @Valid
    List<ProductSkuCreateRequest> productSkus;
}
