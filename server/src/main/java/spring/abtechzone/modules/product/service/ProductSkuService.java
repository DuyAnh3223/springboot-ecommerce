package spring.abtechzone.modules.product.service;

import java.util.List;
import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
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
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductSkuMapper;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.repository.specification.ProductSkuSpecifications;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuService {

    ProductSkuRepository productSkuRepository;
    ProductRepository productRepository;
    ProductSkuMapper productSkuMapper;
    ProductAttributeValidator productAttributeValidator;

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Page<ProductSkuResponse> getSkus(ProductSkuSearchRequest request) {
        Specification<ProductSku> spec = Specification.where(ProductSkuSpecifications.hasKeyword(request.getSearch()))
                .and(ProductSkuSpecifications.hasProductId(request.getProductId()))
                .and(ProductSkuSpecifications.hasMinPrice(request.getMinPrice()))
                .and(ProductSkuSpecifications.hasMaxPrice(request.getMaxPrice()));

        return productSkuRepository.findAll(spec, request.toPageable()).map(productSkuMapper::toProductSkuResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductSkuResponse getSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));
        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse createSku(Long productId, ProductSkuCreateRequest request) {
        validateSkuForCreate(request.getSku());

        Product product =
                productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductSku sku = productSkuMapper.toProductSku(request);
        sku.setProduct(product);
        productAttributeValidator.validateSkuAttributes(product, sku.getAttributes());

        // Validate SKU duplicate variant combination
        productAttributeValidator.validateSkuNotDuplicate(product, product.getSkus(), sku.getAttributes());

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve
                    && "product_sku_sku_active_uq".equals(cve.getConstraintName())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
            throw ex;
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse updateSku(Long skuId, ProductSkuUpdateRequest request) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        validateSkuForUpdate(skuId, request.getSku());

        Map<String, Object> updatedAttributes =
                request.getAttributes() == null ? sku.getAttributes() : request.getAttributes();
        productAttributeValidator.validateSkuAttributes(sku.getProduct(), updatedAttributes);

        // Validate SKU duplicate variant combination for updated attributes
        if (request.getAttributes() != null) {
            List<ProductSku> otherSkus = sku.getProduct().getSkus().stream()
                    .filter(s -> !s.getId().equals(skuId))
                    .toList();
            productAttributeValidator.validateSkuNotDuplicate(sku.getProduct(), otherSkus, updatedAttributes);
        }

        productSkuMapper.updateProductSku(sku, request);

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve
                    && "product_sku_sku_active_uq".equals(cve.getConstraintName())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
            throw ex;
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public void deleteSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        Product product = sku.getProduct();
        if (product.isPublished()) {
            long remainingActive = product.getSkus().stream()
                    .filter(s -> !s.getId().equals(skuId) && Boolean.TRUE.equals(s.getIsActive()))
                    .count();
            if (remainingActive == 0) {
                throw new AppException(ErrorCode.PRODUCT_MUST_HAVE_ACTIVE_SKU);
            }
        }

        sku.softDelete();
        productSkuRepository.save(sku);
    }

    private void validateSkuForCreate(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
        }

        if (productSkuRepository.existsBySku(sku)) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
    }

    private void validateSkuForUpdate(Long skuId, String sku) {
        if (sku == null) {
            return;
        }

        if (sku.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
        }

        if (productSkuRepository.existsBySkuAndIdNot(sku, skuId)) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
    }
}
