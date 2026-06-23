package spring.abtechzone.mapper;

import org.mapstruct.*;

import spring.abtechzone.dto.request.ProductRequest;
import spring.abtechzone.dto.response.ProductResponse;
import spring.abtechzone.entity.Product;

@Mapper(
        componentModel = "spring",
        uses = {ProductSkuMapper.class})
public interface ProductMapper {

    @Mapping(source = "productSkus", target = "skus")
    Product toProduct(ProductRequest productRequest);

    @Mapping(source = "skus", target = "productSkus")
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "skus", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProduct(@MappingTarget Product product, ProductRequest request);

    @AfterMapping
    default void linkSkus(@MappingTarget Product product) {
        if (product.getSkus() != null) {
            product.getSkus().forEach(sku -> sku.setProduct(product));
        }
    }
}
