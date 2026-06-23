package spring.abtechzone.entity;

import java.io.Serializable;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductAttribute implements Serializable {
    String name;
    List<String> values;
}
