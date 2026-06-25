package spring.abtechzone.entity;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAttribute implements Serializable {

    @NotBlank(message = "PRODUCT_ATTRIBUTES_INVALID")
    String name;

    @NotEmpty(message = "PRODUCT_ATTRIBUTES_INVALID")
    List<@NotBlank(message = "PRODUCT_ATTRIBUTES_INVALID") String> values;
}
