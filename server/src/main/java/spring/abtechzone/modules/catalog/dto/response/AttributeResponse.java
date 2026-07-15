package spring.abtechzone.modules.catalog.dto.response;

import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeResponse {
    Long id;
    Long categoryId;
    String code;
    String name;
    String dataType;
    String unit;
    Map<String, Object> enumValues;
    Boolean isFilterable;
    Boolean isVariantDefining;
    Boolean isCompatibilityKey;
    Integer sortOrder;
}
