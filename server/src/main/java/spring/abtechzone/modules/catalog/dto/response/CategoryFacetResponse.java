package spring.abtechzone.modules.catalog.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryFacetResponse {
    Long categoryId;
    String categoryName;
    String categorySlug;

    List<BrandFacetResponse> brands;
    BigDecimal priceMin;
    BigDecimal priceMax;

    List<AttributeFacetResponse> attributes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BrandFacetResponse {
        Long id;
        String name;
        String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttributeFacetResponse {
        Long attributeId;
        String code;
        String name;
        String dataType;
        String unit;
        List<Object> enumValues;
        Boolean isFilterable;
        Boolean isSortable;
        Boolean isVariantDefining;
        Boolean isMultiValue;
        Integer sortOrder;

        // Min/Max bound for NUMBER type attribute across published products
        BigDecimal minBound;
        BigDecimal maxBound;
    }
}
