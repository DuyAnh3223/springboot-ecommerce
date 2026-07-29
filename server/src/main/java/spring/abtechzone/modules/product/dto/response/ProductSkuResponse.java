package spring.abtechzone.modules.product.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSkuResponse {

    Long id;
    Long productId;
    String productName;
    String sku;
    BigDecimal price;
    Integer stock;
    String imageUrl;
    List<ProductImageResponse> images;
    boolean active;
    Map<String, Object> attributes;
}
