package spring.abtechzone.modules.product.mapper;

import org.mapstruct.*;

import spring.abtechzone.modules.category.mapper.BrandMapper;
import spring.abtechzone.modules.category.mapper.CategoryMapper;
import spring.abtechzone.modules.product.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductResponse;
import spring.abtechzone.modules.product.entity.Product;

@Mapper(
        componentModel = "spring",
        uses = {ProductSkuMapper.class, CategoryMapper.class, BrandMapper.class})
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    Product toProduct(ProductCreateRequest productRequest);

    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    ProductResponse toProductResponseSummary(Product product);

    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProduct(@MappingTarget Product product, ProductUpdateRequest request);

    @AfterMapping
    default void linkSkus(@MappingTarget Product product) {
        if (product.getSkus() != null) {
            product.getSkus().forEach(sku -> sku.setProduct(product));
        }
    }
}
