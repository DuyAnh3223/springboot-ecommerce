package spring.abtechzone.modules.catalog.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogProductDetailResponse {

    Long id;
    String name;
    String slug;
    String description;
    String primaryImageUrl;
    Double rating;
    Integer reviewCount;
    Reference category;
    Reference brand;
    Map<String, Object> attributes;
    List<AttributeDefinition> specificationDefinitions;
    List<AttributeDefinition> variantDefinitions;
    BigDecimal priceMin;
    BigDecimal priceMax;
    Integer totalStock;
    List<Sku> skus;

    @Getter
    @Builder
    public static class Reference {
        Long id;
        String name;
        String slug;
    }

    @Getter
    @Builder
    public static class AttributeDefinition {
        String code;
        String name;
        String unit;
        String dataType;
        Integer sortOrder;
    }

    @Getter
    @Builder
    public static class Sku {
        Long id;
        String sku;
        BigDecimal price;
        Integer stock;
        String currency;
        Integer weightGram;
        Map<String, Object> attributes;
        String primaryImageUrl;
        List<Image> images;
    }

    @Getter
    @Builder
    public static class Image {
        Long id;
        String url;
        String altText;
        Integer sortOrder;
        boolean primary;
    }
}
