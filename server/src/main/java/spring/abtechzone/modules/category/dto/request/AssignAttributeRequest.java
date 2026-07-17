package spring.abtechzone.modules.category.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignAttributeRequest {

    Long attributeId;

    @Builder.Default
    Boolean isFilterable = true;

    @Builder.Default
    Boolean isVariantDefining = false;

    @Builder.Default
    Boolean isCompatibilityKey = false;

    @Builder.Default
    Boolean isRequired = false;

    @Builder.Default
    Boolean isMultiValue = false;

    @Builder.Default
    Integer sortOrder = 0;
}
