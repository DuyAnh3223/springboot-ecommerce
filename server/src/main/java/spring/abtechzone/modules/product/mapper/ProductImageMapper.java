package spring.abtechzone.modules.product.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.modules.product.dto.response.ProductImageResponse;
import spring.abtechzone.modules.product.entity.ProductImage;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(source = "isPrimary", target = "primary")
    @Mapping(target = "accessUrl", ignore = true)
    ProductImageResponse toProductImageResponse(ProductImage image, @Context AwsS3FileService awsS3FileService);

    @AfterMapping
    default void resolveAccessUrl(
            ProductImage source,
            @MappingTarget ProductImageResponse target,
            @Context AwsS3FileService awsS3FileService) {
        if (source != null && source.getUrl() != null && awsS3FileService != null) {
            String resolved = awsS3FileService.resolveAccessUrl(source.getUrl());
            target.setAccessUrl(resolved != null ? resolved : source.getUrl());
        }
    }
}
