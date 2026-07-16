package spring.abtechzone.modules.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.catalog.dto.request.AttributeRequest;
import spring.abtechzone.modules.catalog.dto.response.AttributeResponse;
import spring.abtechzone.modules.catalog.entity.Attribute;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

    Attribute toAttribute(AttributeRequest request);

    AttributeResponse toAttributeResponse(Attribute attribute);

    void updateAttribute(@MappingTarget Attribute attribute, AttributeRequest request);
}
