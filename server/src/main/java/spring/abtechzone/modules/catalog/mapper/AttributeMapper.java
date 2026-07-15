package spring.abtechzone.modules.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.catalog.dto.request.AttributeRequest;
import spring.abtechzone.modules.catalog.dto.response.AttributeResponse;
import spring.abtechzone.modules.catalog.entity.Attribute;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

    @Mapping(target = "category", ignore = true)
    Attribute toAttribute(AttributeRequest request);

    @Mapping(source = "category.id", target = "categoryId")
    AttributeResponse toAttributeResponse(Attribute attribute);

    @Mapping(target = "category", ignore = true)
    void updateAttribute(@MappingTarget Attribute attribute, AttributeRequest request);
}
