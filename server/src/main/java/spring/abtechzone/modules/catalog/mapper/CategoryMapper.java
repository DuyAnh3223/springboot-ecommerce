package spring.abtechzone.modules.catalog.mapper;

import org.mapstruct.Mapper;

import spring.abtechzone.modules.catalog.dto.request.CategoryRequest;
import spring.abtechzone.modules.catalog.dto.response.CategoryResponse;
import spring.abtechzone.modules.catalog.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toCategory(CategoryRequest request);

    CategoryResponse toCategoryResponse(Category category);
}
