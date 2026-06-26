package spring.abtechzone.modules.product.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.PositiveOrZero;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSkuUpdateRequest {

    String sku;

    @PositiveOrZero(message = "PRODUCT_PRICE_INVALID")
    BigDecimal price;

    @PositiveOrZero(message = "PRODUCT_STOCK_INVALID")
    Integer stock;

    String imageUrl;
    Map<String, String> attributes;
}
