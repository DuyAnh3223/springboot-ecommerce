package spring.abtechzone.service;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.dto.response.ProductSkuResponse;
import spring.abtechzone.entity.Product;
import spring.abtechzone.entity.ProductSku;
import spring.abtechzone.exception.AppException;
import spring.abtechzone.exception.ErrorCode;
import spring.abtechzone.mapper.ProductSkuMapper;
import spring.abtechzone.repository.ProductRepository;
import spring.abtechzone.repository.ProductSkuRepository;
import spring.abtechzone.service.validator.ProductAttributeValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuService {

    ProductSkuRepository productSkuRepository;
    ProductRepository productRepository;
    ProductSkuMapper productSkuMapper;
    ProductAttributeValidator productAttributeValidator;

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

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse updateSku(Long skuId, ProductSkuUpdateRequest request) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        // Cập nhật các trường cụ thể
        validateSkuForUpdate(skuId, request.getSku());

        Map<String, String> updatedAttributes =
                request.getAttributes() == null ? sku.getAttributes() : request.getAttributes();
        productAttributeValidator.validateSkuAttributes(sku.getProduct(), updatedAttributes);

        productSkuMapper.updateProductSku(sku, request);

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public void deleteSku(Long skuId) {
        if (!productSkuRepository.existsById(skuId)) {
            throw new AppException(ErrorCode.SKU_NOT_FOUND);
        }
        productSkuRepository.deleteById(skuId);
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
