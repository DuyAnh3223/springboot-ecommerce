package spring.abtechzone.modules.catalog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.modules.catalog.dto.request.AssignAttributeRequest;
import spring.abtechzone.modules.catalog.dto.response.CategoryAttributeResponse;
import spring.abtechzone.modules.catalog.entity.CategoryAttribute;

@Mapper(componentModel = "spring")
public interface CategoryAttributeMapper {

    @Mapping(target = "attributeId", source = "attribute.id")
    @Mapping(target = "code", source = "attribute.code")
    @Mapping(target = "name", source = "attribute.name")
    @Mapping(target = "dataType", source = "attribute.dataType")
    @Mapping(target = "unit", source = "attribute.unit")
    @Mapping(target = "enumValues", source = "attribute.enumValues")
    CategoryAttributeResponse toCategoryAttributeResponse(CategoryAttribute categoryAttribute);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "attribute", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateCategoryAttribute(@MappingTarget CategoryAttribute categoryAttribute, AssignAttributeRequest request);
}
