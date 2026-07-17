package spring.abtechzone.modules.category.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.modules.category.dto.request.BrandRequest;
import spring.abtechzone.modules.category.dto.response.BrandResponse;
import spring.abtechzone.modules.category.entity.Brand;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toBrand(BrandRequest request);

    BrandResponse toBrandResponse(Brand brand);
}
