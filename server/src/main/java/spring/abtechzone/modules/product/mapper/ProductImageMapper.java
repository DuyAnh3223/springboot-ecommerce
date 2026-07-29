package spring.abtechzone.modules.product.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.product.dto.response.ProductImageResponse;
import spring.abtechzone.modules.product.entity.ProductImage;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(target = "accessUrl", ignore = true)
    ProductImageResponse toProductImageResponse(ProductImage image);
}
