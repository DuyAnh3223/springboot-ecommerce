package spring.abtechzone.modules.product.dto.request;

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

    String description;

    boolean draft;

    boolean published;

    Long categoryId;
    Long brandId;

    @Valid
    Map<String, Object> attributes;

    @Valid
    List<ProductSkuCreateRequest> skus;
}
