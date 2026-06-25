package spring.abtechzone.mapper;

import org.mapstruct.*;

import spring.abtechzone.dto.request.ProductCreateRequest;
import spring.abtechzone.dto.request.ProductUpdateRequest;
import spring.abtechzone.dto.response.ProductResponse;
import spring.abtechzone.entity.Product;

@Mapper(
        componentModel = "spring",
        uses = {ProductSkuMapper.class})
public interface ProductMapper {

    @Mapping(source = "productSkus", target = "skus")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    Product toProduct(ProductCreateRequest productRequest);

    @Mapping(source = "skus", target = "productSkus")
    @Mapping(source = "draft", target = "isDraft")
    @Mapping(source = "published", target = "isPublished")
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(source = "isDraft", target = "draft")
    @Mapping(source = "isPublished", target = "published")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProduct(@MappingTarget Product product, ProductUpdateRequest request);

    @AfterMapping
    default void linkSkus(ProductCreateRequest request, @MappingTarget Product product) {
        if (product.getSkus() != null) {
            product.getSkus().forEach(sku -> sku.setProduct(product));
        }
    }
}
