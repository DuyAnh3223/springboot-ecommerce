package spring.abtechzone.modules.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.category.dto.request.AttributeRequest;
import spring.abtechzone.modules.category.dto.response.AttributeResponse;
import spring.abtechzone.modules.category.entity.Attribute;

@Mapper(componentModel = "spring")
public interface AttributeMapper {

    Attribute toAttribute(AttributeRequest request);

    AttributeResponse toAttributeResponse(Attribute attribute);

    void updateAttribute(@MappingTarget Attribute attribute, AttributeRequest request);
}
