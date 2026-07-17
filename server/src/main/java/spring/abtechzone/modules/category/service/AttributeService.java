package spring.abtechzone.modules.category.service;

import java.time.OffsetDateTime;

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
import spring.abtechzone.modules.category.dto.request.AttributeRequest;
import spring.abtechzone.modules.category.dto.request.AttributeSearchRequest;
import spring.abtechzone.modules.category.dto.response.AttributeResponse;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.mapper.AttributeMapper;
import spring.abtechzone.modules.category.repository.AttributeRepository;
import spring.abtechzone.modules.category.repository.specification.AttributeSpecifications;

@Service
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttributeService {

    AttributeRepository attributeRepository;
    AttributeMapper attributeMapper;

    public AttributeResponse createAttributeDefinition(AttributeRequest request) {
        if (attributeRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.ATTRIBUTE_EXISTS);
        }

        Attribute attribute = attributeMapper.toAttribute(request);
        attribute.setCreatedAt(OffsetDateTime.now());
        attribute.setUpdatedAt(OffsetDateTime.now());
        attributeRepository.save(attribute);

        return attributeMapper.toAttributeResponse(attribute);
    }

    @PreAuthorize("permitAll()")
    @Transactional(readOnly = true)
    public Page<AttributeResponse> getGlobalAttributes(AttributeSearchRequest request) {
        Specification<Attribute> spec = Specification.where(AttributeSpecifications.hasKeyword(request.getKeyword()));

        return attributeRepository.findAll(spec, request.toPageable()).map(attributeMapper::toAttributeResponse);
    }

    @PreAuthorize("permitAll()")
    public AttributeResponse getAttribute(Long attributeId) {
        Attribute attribute = attributeRepository
                .findById(attributeId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND));
        return attributeMapper.toAttributeResponse(attribute);
    }

    public AttributeResponse updateAttribute(Long attributeId, AttributeRequest request) {
        Attribute attribute = attributeRepository
                .findById(attributeId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND));

        // Check duplicate code
        if (attributeRepository.existsByCodeAndIdNot(request.getCode(), attributeId)) {
            throw new AppException(ErrorCode.ATTRIBUTE_EXISTS);
        }

        attributeMapper.updateAttribute(attribute, request);
        attribute.setUpdatedAt(OffsetDateTime.now());
        attributeRepository.save(attribute);

        return attributeMapper.toAttributeResponse(attribute);
    }

    public void deleteAttribute(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new AppException(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }
        attributeRepository.deleteById(attributeId);
        log.info("Deleted attribute id={}", attributeId);
    }
}
