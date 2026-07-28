package spring.abtechzone.modules.product.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.PositiveOrZero;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSkuUpdateRequest {

    String sku;

    @PositiveOrZero(message = "PRODUCT_PRICE_INVALID")
    BigDecimal price;

    @PositiveOrZero(message = "PRODUCT_STOCK_INVALID")
    Integer stock;

    String currency;

    Integer weightGram;

    List<ProductImageRequest> images;

    Boolean isActive;
    Map<String, Object> attributes;

    @JsonGetter("images")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<ProductImageRequest> getImages() {
        return images;
    }

    public void setImages(List<ProductImageRequest> images) {
        if (images == null) {
            throw new IllegalArgumentException("images must be omitted or an array");
        }
        this.images = images;
    }
}
