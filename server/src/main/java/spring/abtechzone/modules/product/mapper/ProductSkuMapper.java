package spring.abtechzone.modules.product.mapper;

import org.mapstruct.*;

import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.entity.ProductSku;

@Mapper(
        componentModel = "spring",
        uses = {ProductImageMapper.class})
public interface ProductSkuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    ProductSku toProductSku(ProductSkuCreateRequest productSkuRequest);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    ProductSkuResponse toProductSkuResponse(ProductSku productSku);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductSku(@MappingTarget ProductSku productSku, ProductSkuUpdateRequest request);
}
