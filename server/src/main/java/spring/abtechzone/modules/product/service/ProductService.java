package spring.abtechzone.modules.product.service;

import java.util.HashSet;
import java.util.Set;

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
import spring.abtechzone.modules.category.entity.Brand;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.BrandRepository;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.product.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductResponse;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.mapper.ProductMapper;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.repository.specification.ProductSpecifications;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductSkuRepository productSkuRepository;
    ProductMapper productMapper;
    ProductAttributeValidator productAttributeValidator;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;

    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        if (request.getCategoryId() == null) {
            throw new AppException(ErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }

        validateSkusForCreate(request);

        Product product = productMapper.toProduct(request);

        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        product.setCategory(category);

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId()).orElse(null);
            product.setBrand(brand);
        }

        // Validate Slug uniqueness
        String slug = product.generateSlug(product.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
        }

        productAttributeValidator.validateProductAttributes(product);
        productAttributeValidator.validateProductSkus(product);

        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve
                    && "product_slug_active_uq".equals(cve.getConstraintName())) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
            }
            throw ex;
        }

        return productMapper.toProductResponse(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @PreAuthorize("permitAll()")
    public ProductResponse getProduct(Long id) {
        return productMapper.toProductResponse(findProductById(id));
    }
    
    @PreAuthorize("permitAll()")
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

        if (request.getCategoryId() != null
                && !request.getCategoryId().equals(product.getCategory().getId())) {
            throw new AppException(ErrorCode.PRODUCT_CATEGORY_CANNOT_BE_CHANGED);
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId()).orElse(null);
            product.setBrand(brand);
        }

        // Validate Slug uniqueness if name is updated
        if (request.getName() != null
                && !request.getName().isBlank()
                && !request.getName().equals(product.getName())) {
            String newSlug = product.generateSlug(request.getName());
            if (productRepository.existsBySlugAndIdNot(newSlug, product.getId())) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
            }
        }

        productMapper.updateProduct(product, request);

        // Validate final state of attributes
        productAttributeValidator.validateProductAttributes(product);
        productAttributeValidator.validateProductSkus(product);

        try {
            product = productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve
                    && "product_slug_active_uq".equals(cve.getConstraintName())) {
                throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
            }
            throw ex;
        }

        return productMapper.toProductResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProductById(id);
        product.softDelete();
        productRepository.save(product);
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
                throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
            }

            if (!skus.add(skuRequest.getSku()) || productSkuRepository.existsBySku(skuRequest.getSku())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
        }
    }
}
