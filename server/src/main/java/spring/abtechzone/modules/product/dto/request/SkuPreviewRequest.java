package spring.abtechzone.modules.product.dto.request;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuPreviewRequest {

    @NotEmpty(message = "Product attributes are required for preview")
    Map<String, List<Object>> attributes;
}
