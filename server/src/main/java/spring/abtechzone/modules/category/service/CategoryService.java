package spring.abtechzone.modules.category.service;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.modules.category.dto.request.CategoryRequest;
import spring.abtechzone.modules.category.dto.request.CategorySearchRequest;
import spring.abtechzone.modules.category.dto.response.CategoryResponse;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.mapper.CategoryMapper;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.category.repository.specification.CategorySpecifications;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    AwsS3FileService awsS3FileService;

    public CategoryResponse create(CategoryRequest request) {
        Boolean existedCategory = categoryRepository.existsByName(request.getName());
        if (Boolean.TRUE.equals(existedCategory)) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }
        Category category = categoryMapper.toCategory(request);
        category.setThumbnail(awsS3FileService.extractS3Key(request.getThumbnail()));
        categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(category, awsS3FileService);
    }

    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategories(CategorySearchRequest request) {
        Specification<Category> spec = Specification.where(CategorySpecifications.hasKeyword(request.getKeyword()))
                .and(CategorySpecifications.isActive(request.getIsActive()))
                .and(CategorySpecifications.hasParent(request.getParentId()));

        return categoryRepository
                .findAll(spec, request.toPageable())
                .map(category -> categoryMapper.toCategoryResponse(category, awsS3FileService));
    }

    @PreAuthorize("permitAll()")
    public CategoryResponse getCategory(Long id) {
        Category category =
                categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toCategoryResponse(category, awsS3FileService);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category =
                categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        String oldThumbnail = category.getThumbnail();
        String newThumbnail = awsS3FileService.extractS3Key(request.getThumbnail());

        if (oldThumbnail != null && !oldThumbnail.isBlank() && !oldThumbnail.equals(newThumbnail)) {
            deleteS3ObjectAfterCommit(oldThumbnail);
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setThumbnail(newThumbnail);
        categoryRepository.save(category);

        return categoryMapper.toCategoryResponse(category, awsS3FileService);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category =
                categoryRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        String oldThumbnail = category.getThumbnail();
        if (oldThumbnail != null && !oldThumbnail.isBlank()) {
            deleteS3ObjectAfterCommit(oldThumbnail);
            category.setThumbnail(null);
        }

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private void deleteS3ObjectAfterCommit(String thumbnail) {
        String s3Key = awsS3FileService.extractS3Key(thumbnail);
        if (s3Key == null || s3Key.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        awsS3FileService.deleteObject(s3Key);
                    } catch (Exception e) {
                        log.error("Failed to delete S3 object after commit for key {}: {}", s3Key, e.getMessage(), e);
                    }
                }
            });
        } else {
            try {
                awsS3FileService.deleteObject(s3Key);
            } catch (Exception e) {
                log.error("Failed to delete S3 object for key {}: {}", s3Key, e.getMessage(), e);
            }
        }
    }
}
