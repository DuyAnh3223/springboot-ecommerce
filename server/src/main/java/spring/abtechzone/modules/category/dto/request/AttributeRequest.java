package spring.abtechzone.modules.category.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeRequest {

    @NotBlank(message = "Attribute code is required")
    @Size(max = 100, message = "Attribute code must be at most 100 characters")
    String code;

    @NotBlank(message = "Attribute name is required")
    @Size(max = 150, message = "Attribute name must be at most 150 characters")
    String name;

    @NotBlank(message = "Data type is required")
    @Size(max = 20, message = "Data type must be at most 20 characters")
    String dataType;

    @Size(max = 20, message = "Unit must be at most 20 characters")
    String unit;

    List<Object> enumValues;
}
