package spring.abtechzone.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.dto.request.ProductSkuRequest;
import spring.abtechzone.dto.response.ProductSkuResponse;
import spring.abtechzone.entity.Product;
import spring.abtechzone.entity.ProductSku;
import spring.abtechzone.exception.AppException;
import spring.abtechzone.exception.ErrorCode;
import spring.abtechzone.mapper.ProductSkuMapper;
import spring.abtechzone.repository.ProductRepository;
import spring.abtechzone.repository.ProductSkuRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuService {

    ProductSkuRepository productSkuRepository;
    ProductRepository productRepository;
    ProductSkuMapper productSkuMapper;

    public ProductSkuResponse getSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));
        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse createSku(Long productId, ProductSkuRequest request) {
        Product product =
                productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductSku sku = productSkuMapper.toProductSku(request);
        sku.setProduct(product);

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse updateSku(Long skuId, ProductSkuRequest request) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        // Cập nhật các trường cụ thể
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        if (request.getSku() != null) {
            sku.setSku(request.getSku());
        }
        if (request.getImageUrl() != null) {
            sku.setImageUrl(request.getImageUrl());
        }
        if (request.getAttributes() != null) {
            sku.setAttributes(request.getAttributes());
        }

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
}
