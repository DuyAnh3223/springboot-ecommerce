package spring.abtechzone.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSkuRequest {

    String sku;
    BigDecimal price;
    Integer stock;
    String imageUrl;
    Map<String, String> attributes;
}
