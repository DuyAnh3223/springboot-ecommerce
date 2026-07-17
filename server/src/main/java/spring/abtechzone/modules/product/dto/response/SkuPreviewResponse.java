package spring.abtechzone.modules.product.dto.response;

import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SkuPreviewResponse {

    Map<String, Object> attributes;
}
