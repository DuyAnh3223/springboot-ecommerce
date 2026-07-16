package spring.abtechzone.modules.catalog.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.dto.request.AssignAttributeRequest;
import spring.abtechzone.modules.catalog.dto.response.CategoryAttributeResponse;
import spring.abtechzone.modules.catalog.entity.Attribute;
import spring.abtechzone.modules.catalog.entity.Category;
import spring.abtechzone.modules.catalog.entity.CategoryAttribute;
import spring.abtechzone.modules.catalog.mapper.CategoryAttributeMapper;
import spring.abtechzone.modules.catalog.repository.AttributeRepository;
import spring.abtechzone.modules.catalog.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.catalog.repository.CategoryRepository;

@Service
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryAttributeService {

    CategoryAttributeRepository categoryAttributeRepository;
    CategoryRepository categoryRepository;
    AttributeRepository attributeRepository;
    CategoryAttributeMapper categoryAttributeMapper;

    @Transactional
    public List<CategoryAttributeResponse> assignAttributes(Long categoryId, List<AssignAttributeRequest> requests) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Validate all before insert for atomicity
        for (AssignAttributeRequest req : requests) {
            if (categoryAttributeRepository.existsByCategory_IdAndAttribute_Id(categoryId, req.getAttributeId())) {
                throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_ALREADY_ASSIGNED);
            }
        }

        List<CategoryAttribute> saved = new ArrayList<>();
        for (AssignAttributeRequest req : requests) {
            Attribute attribute = attributeRepository
                    .findById(req.getAttributeId())
                    .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND));

            CategoryAttribute ca = new CategoryAttribute();
            ca.setCategory(category);
            ca.setAttribute(attribute);
            ca.setIsFilterable(req.getIsFilterable() != null ? req.getIsFilterable() : true);
            ca.setIsVariantDefining(req.getIsVariantDefining() != null ? req.getIsVariantDefining() : false);
            ca.setIsCompatibilityKey(req.getIsCompatibilityKey() != null ? req.getIsCompatibilityKey() : false);
            ca.setIsRequired(req.getIsRequired() != null ? req.getIsRequired() : false);
            ca.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
            ca.setCreatedAt(OffsetDateTime.now());
            ca.setUpdatedAt(OffsetDateTime.now());

            saved.add(categoryAttributeRepository.save(ca));
        }

        return saved.stream()
                .map(categoryAttributeMapper::toCategoryAttributeResponse)
                .toList();
    }

    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> getAttributesByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryAttributeRepository.findByCategoryIdWithAttribute(categoryId).stream()
                .map(categoryAttributeMapper::toCategoryAttributeResponse)
                .toList();
    }

    @Transactional
    public CategoryAttributeResponse updateCategoryAttribute(Long id, AssignAttributeRequest request) {
        CategoryAttribute ca = categoryAttributeRepository
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND));

        categoryAttributeMapper.updateCategoryAttribute(ca, request);
        ca.setUpdatedAt(OffsetDateTime.now());
        categoryAttributeRepository.save(ca);

        return categoryAttributeMapper.toCategoryAttributeResponse(ca);
    }

    @Transactional
    public void removeCategoryAttribute(Long categoryId, Long attributeId) {
        if (!categoryAttributeRepository.existsByCategory_IdAndAttribute_Id(categoryId, attributeId)) {
            throw new AppException(ErrorCode.CATEGORY_ATTRIBUTE_NOT_FOUND);
        }
        categoryAttributeRepository.deleteByCategory_IdAndAttribute_Id(categoryId, attributeId);
    }
}
