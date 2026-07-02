package spring.abtechzone.modules.catalog.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductSearchRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.catalog.dto.response.ProductResponse;
import spring.abtechzone.modules.catalog.entity.Brand;
import spring.abtechzone.modules.catalog.entity.Category;
import spring.abtechzone.modules.catalog.entity.Product;
import spring.abtechzone.modules.catalog.mapper.ProductMapper;
import spring.abtechzone.modules.catalog.repository.ProductRepository;
import spring.abtechzone.modules.catalog.repository.ProductSkuRepository;
import spring.abtechzone.modules.catalog.repository.specification.ProductSpecifications;
import spring.abtechzone.modules.catalog.validator.ProductAttributeValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductSkuRepository productSkuRepository;
    ProductMapper productMapper;
    ProductAttributeValidator productAttributeValidator;
    spring.abtechzone.modules.catalog.repository.CategoryRepository categoryRepository;
    spring.abtechzone.modules.catalog.repository.BrandRepository brandRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        validateSkusForCreate(request);

        Product product = productMapper.toProduct(request);

        if (request.getCategoryId() != null) {
           Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }
        if (request.getBrandId() != null) {
            Brand brand =
                    brandRepository.findById(request.getBrandId()).orElse(null);
            product.setBrand(brand);
        }

        productAttributeValidator.validateProductAttributes(product);
        productAttributeValidator.validateProductSkus(product);

        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            handleProductSaveException(ex);
        }

        return productMapper.toProductResponse(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse getProduct(Long id) {
        return productMapper.toProductResponse(findProductById(id));
    }

    public Page<ProductResponse> getProducts(ProductSearchRequest request) {

        Specification<Product> spec = Specification.where(ProductSpecifications.isPublished())
                .and(ProductSpecifications.hasKeyword(request.getSearch()));

        Page<Product> productsPage = productRepository.findAll(spec, request.toPageable());

        return productsPage.map(productMapper::toProductResponse);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {

        Product product = findProductById(id);

        validateProductFieldsForUpdate(request);
        productAttributeValidator.validateExistingSkusAgainstUpdatedAttributes(product, request.getAttributes());

        productMapper.updateProduct(product, request);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository
                    .findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }
        if (request.getBrandId() != null) {
            Brand brand =
                    brandRepository.findById(request.getBrandId()).orElse(null);
            product.setBrand(brand);
        }

        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            handleProductSaveException(ex);
        }

        return productMapper.toProductResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private void validateProductFieldsForUpdate(ProductUpdateRequest request) {
        if (request.getName() != null && request.getName().isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_INVALID);
        }
    }

    private void validateSkusForCreate(ProductCreateRequest request) {
        if (request.getProductSkus() == null) {
            return;
        }

        Set<String> skus = new HashSet<>();
        for (ProductSkuCreateRequest skuRequest : request.getProductSkus()) {
            if (skuRequest.getSku() == null || skuRequest.getSku().isBlank()) {
                continue;
            }

            if (!skus.add(skuRequest.getSku()) || productSkuRepository.existsBySku(skuRequest.getSku())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
        }
    }

    private void handleProductSaveException(DataIntegrityViolationException ex) {
        String msg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        if (msg != null && (msg.contains("product_sku") || msg.toLowerCase().contains("sku"))) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
        throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
    }
}
