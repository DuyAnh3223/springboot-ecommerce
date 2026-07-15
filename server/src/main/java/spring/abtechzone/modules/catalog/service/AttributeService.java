package spring.abtechzone.modules.catalog.service;

import java.text.Normalizer;
import java.util.Locale;

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
import spring.abtechzone.modules.catalog.dto.request.AttributeRequest;
import spring.abtechzone.modules.catalog.dto.request.AttributeSearchRequest;
import spring.abtechzone.modules.catalog.dto.response.AttributeResponse;
import spring.abtechzone.modules.catalog.entity.Attribute;
import spring.abtechzone.modules.catalog.entity.Category;
import spring.abtechzone.modules.catalog.mapper.AttributeMapper;
import spring.abtechzone.modules.catalog.repository.AttributeRepository;
import spring.abtechzone.modules.catalog.repository.CategoryRepository;
import spring.abtechzone.modules.catalog.repository.specification.AttributeSpecifications;

@Service
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttributeService {

    AttributeRepository attributeRepository;
    AttributeMapper attributeMapper;
    CategoryRepository categoryRepository;

    @Transactional
    public AttributeResponse createAttributeDefinition(AttributeRequest attributeRequest) {
        String code = convertNametoCode(attributeRequest.getName());
        boolean existed = attributeRepository.existsByCode(code);
        if (existed) {
            throw new AppException(ErrorCode.ATTRIBUTE_EXISTS);
        }

        Category category = categoryRepository
                .findById(attributeRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Attribute attribute = attributeMapper.toAttribute(attributeRequest);
        attribute.setCode(code);
        attribute.setCategory(category);

        attributeRepository.save(attribute);
        return attributeMapper.toAttributeResponse(attribute);
    }

    private String convertNametoCode(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String target = name.trim().toLowerCase(Locale.ROOT);
        target = target.replace('đ', 'd');
        target = Normalizer.normalize(target, Normalizer.Form.NFD);
        target = target.replaceAll("\\p{M}", "");
        target = target.replaceAll("[\\s\\-\\.]+", "_");
        target = target.replaceAll("[^a-z0-9_]", "");
        target = target.replaceAll("_+", "_");
        target = target.replaceAll("^_+|_+$", "");
        return target;
    }

    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public Page<AttributeResponse> getAttributesByCategoryId(Long categoryId, AttributeSearchRequest request) {
        Specification<Attribute> spec = Specification.where(AttributeSpecifications.hasCategoryId(categoryId))
                .and(AttributeSpecifications.hasKeyword(request.getKeyword()))
                .and(AttributeSpecifications.isVariantDefining(request.getIsVariantDefining()))
                .and(AttributeSpecifications.isCompatibilityKey(request.getIsCompatibilityKey()))
                .and(AttributeSpecifications.isFilterable(request.getIsFilterable()));

        return attributeRepository.findAll(spec, request.toPageable()).map(attributeMapper::toAttributeResponse);
    }

    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public AttributeResponse getAttribute(Long id) {
        return attributeMapper.toAttributeResponse(
                attributeRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND)));
    }

    @Transactional
    public AttributeResponse updateAttribute(Long id, AttributeRequest request) {
        Attribute attribute =
                attributeRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND));

        attributeMapper.updateAttribute(attribute, request);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            attribute.setCategory(category);
        }

        attributeRepository.save(attribute);
        return attributeMapper.toAttributeResponse(attribute);
    }

    @Transactional
    public void deleteAttribute(Long id) {
        Attribute attribute =
                attributeRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND));
        attributeRepository.delete(attribute);
    }
}
