package spring.abtechzone.modules.catalog.dto.request;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

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

    Long categoryId;
    Long brandId;

    @Valid
    Map<String, Object> attributes;

    @Valid
    List<ProductSkuCreateRequest> productSkus;
}
