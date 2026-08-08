package spring.abtechzone.modules.product.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.category.dto.response.BrandResponse;
import spring.abtechzone.modules.category.dto.response.CategoryResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {

    Long id;
    String name;
    String slug;
    String primaryImageUrl;
    String description;

    Double rating;

    boolean draft;
    boolean published;

    Integer skuCount;
    Integer activeSkuCount;
    Integer totalStock;
    Long singleSkuId;
    BigDecimal priceMin;
    BigDecimal priceMax;

    CategoryResponse category;
    BrandResponse brand;

    Map<String, Object> attributes;

    List<ProductSkuResponse> skus;
}
