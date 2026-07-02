package spring.abtechzone.modules.catalog.mapper;

import org.mapstruct.*;

import spring.abtechzone.modules.catalog.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.catalog.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.catalog.entity.ProductSku;

@Mapper(componentModel = "spring")
public interface ProductSkuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductSku toProductSku(ProductSkuCreateRequest productSkuRequest);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    ProductSkuResponse toProductSkuResponse(ProductSku productSku);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductSku(@MappingTarget ProductSku productSku, ProductSkuUpdateRequest request);
}
