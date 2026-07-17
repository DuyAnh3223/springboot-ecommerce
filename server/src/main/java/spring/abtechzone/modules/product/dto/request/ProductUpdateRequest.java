package spring.abtechzone.modules.product.dto.request;

import java.util.Map;

import jakarta.validation.Valid;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductUpdateRequest {

    String name;
    String thumbnail;
    String description;

    Boolean isDraft;
    Boolean isPublished;

    Long categoryId;
    Long brandId;

    @Valid
    Map<String, Object> attributes;
}
