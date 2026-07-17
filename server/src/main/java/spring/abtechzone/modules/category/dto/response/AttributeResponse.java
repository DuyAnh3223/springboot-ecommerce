package spring.abtechzone.modules.category.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeResponse {
    Long id;
    String code;
    String name;
    String dataType;
    String unit;
    List<Object> enumValues;
}
