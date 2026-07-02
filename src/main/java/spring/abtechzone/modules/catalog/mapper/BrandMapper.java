package spring.abtechzone.modules.catalog.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.modules.catalog.dto.request.BrandRequest;
import spring.abtechzone.modules.catalog.dto.response.BrandResponse;
import spring.abtechzone.modules.catalog.entity.Brand;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toBrand(BrandRequest request);

    BrandResponse toBrandResponse(Brand brand);
}
