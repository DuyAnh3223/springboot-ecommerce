package spring.abtechzone.modules.category.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.modules.category.dto.request.CategoryRequest;
import spring.abtechzone.modules.category.dto.response.CategoryResponse;
import spring.abtechzone.modules.category.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toCategory(CategoryRequest request);

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "thumbnail", ignore = true)
    CategoryResponse toCategoryResponse(Category category, @Context AwsS3FileService awsS3FileService);

    @AfterMapping
    default void resolveThumbnail(
            Category source, @MappingTarget CategoryResponse target, @Context AwsS3FileService awsS3FileService) {
        if (source != null && source.getThumbnail() != null && awsS3FileService != null) {
            target.setThumbnail(awsS3FileService.resolveAccessUrl(source.getThumbnail()));
        }
    }
}
