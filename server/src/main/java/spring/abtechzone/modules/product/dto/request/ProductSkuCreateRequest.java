package spring.abtechzone.modules.product.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductSkuCreateRequest {

    @NotNull(message = "PRODUCT_NOT_FOUND")
    Long productId;

    @NotBlank(message = "PRODUCT_SKU_INVALID")
    String sku;

    @NotNull(message = "PRODUCT_PRICE_INVALID")
    @PositiveOrZero(message = "PRODUCT_PRICE_INVALID")
    BigDecimal price;

    @NotNull(message = "PRODUCT_STOCK_INVALID")
    @PositiveOrZero(message = "PRODUCT_STOCK_INVALID")
    Integer stock;

    String currency;

    Integer weightGram;

    String imageUrl;
    Map<String, Object> attributes;
}
