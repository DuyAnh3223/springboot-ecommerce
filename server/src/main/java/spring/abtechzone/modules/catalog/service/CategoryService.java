package spring.abtechzone.modules.catalog.service;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.dto.request.CategoryRequest;
import spring.abtechzone.modules.catalog.dto.request.CategorySearchRequest;
import spring.abtechzone.modules.catalog.dto.response.CategoryResponse;
import spring.abtechzone.modules.catalog.entity.Category;
import spring.abtechzone.modules.catalog.mapper.CategoryMapper;
import spring.abtechzone.modules.catalog.repository.CategoryRepository;
import spring.abtechzone.modules.catalog.repository.specification.CategorySpecifications;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    public CategoryResponse create(CategoryRequest request) {
        Boolean existedCategory = categoryRepository.existsByName(categoryMapper.toCategory(request));
        if (Boolean.TRUE.equals(existedCategory)) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }
        Category category = categoryMapper.toCategory(request);
        categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(category);
    }

    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategories(CategorySearchRequest request) {
        Specification<Category> spec = Specification
                .where(CategorySpecifications.hasKeyword(request.getKeyword()))
                .and(CategorySpecifications.isActive(request.getIsActive()))
                .and(CategorySpecifications.hasParent(request.getParentId()));

        return categoryRepository
                .findAll(spec, request.toPageable())
                .map(categoryMapper::toCategoryResponse);
    }

    @PreAuthorize("permitAll()")
    public CategoryResponse getCategory(Long id) {
        return categoryMapper.toCategoryResponse(
                categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND)));
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category =
                categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setThumbnail(request.getThumbnail());
        categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(category);
    }

    public void deleteCategory(Long id) {
        Category category =
                categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setIsActive(false);
        categoryRepository.save(category);
    }
}
