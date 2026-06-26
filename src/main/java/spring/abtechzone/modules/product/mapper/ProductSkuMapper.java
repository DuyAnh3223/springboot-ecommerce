package spring.abtechzone.modules.product.mapper;

import org.mapstruct.*;

import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.entity.ProductSku;

@Mapper(componentModel = "spring")
public interface ProductSkuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductSku toProductSku(ProductSkuCreateRequest productSkuRequest);

    ProductSkuResponse toProductSkuResponse(ProductSku productSku);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductSku(@MappingTarget ProductSku productSku, ProductSkuUpdateRequest request);
}
