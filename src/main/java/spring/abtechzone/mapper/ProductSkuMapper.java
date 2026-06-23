package spring.abtechzone.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.dto.request.ProductSkuRequest;
import spring.abtechzone.dto.response.ProductSkuResponse;
import spring.abtechzone.entity.ProductSku;

@Mapper(componentModel = "spring")
public interface ProductSkuMapper {

    ProductSku toProductSku(ProductSkuRequest productSkuRequest);

    ProductSkuResponse toProductSkuResponse(ProductSku productSku);
}
