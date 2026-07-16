package spring.abtechzone.modules.catalog.dto.response;

import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryAttributeResponse {

    Long id;

    Long attributeId;
    String code;
    String name;
    String dataType;
    String unit;
    Map<String, Object> enumValues;

    Boolean isFilterable;
    Boolean isVariantDefining;
    Boolean isCompatibilityKey;
    Boolean isRequired;
    Integer sortOrder;
}
