package spring.abtechzone.modules.product.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import spring.abtechzone.common.service.AwsS3FileService;
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
import spring.abtechzone.modules.product.entity.ProductImage;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductMapper;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.repository.specification.ProductSpecifications;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductService {
    ProductRepository productRepository;
    ProductSkuRepository productSkuRepository;
    ProductImageRepository productImageRepository;
    ProductMapper productMapper;
    ProductSkuService productSkuService;
    ProductAttributeValidator productAttributeValidator;
    CategoryRepository categoryRepository;
    BrandRepository brandRepository;
    AwsS3FileService awsS3FileService;

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
            Brand brand = brandRepository
                    .findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
            product.setBrand(brand);
        }

        // Validate Slug uniqueness
        String slug = product.generateSlug(product.getName());
        if (productRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
        }

        productAttributeValidator.validateProductAttributes(product);
        productAttributeValidator.validateProductSkus(product);

        product = productRepository.save(product);

        return toDetailResponse(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @PreAuthorize("permitAll()")
    public ProductResponse getProduct(Long id) {
        return toDetailResponse(findProductById(id));
    }

    @PreAuthorize("permitAll()")
    public ProductResponse getProductBySlug(String slug) {
        Product product =
                productRepository.findBySlug(slug).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDraft() || !product.isPublished()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        ProductResponse response = toDetailResponse(product);
        if (response.getSkus() != null) {
            List<spring.abtechzone.modules.product.dto.response.ProductSkuResponse> activeSkus =
                    response.getSkus().stream()
                            .filter(spring.abtechzone.modules.product.dto.response.ProductSkuResponse::isActive)
                            .toList();
            response.setSkus(activeSkus);
        }
        return response;
    }

    @PreAuthorize("permitAll()")
    public Page<ProductResponse> getProducts(ProductSearchRequest request) {
        Specification<Product> spec = Specification.where(ProductSpecifications.isPublished())
                .and(ProductSpecifications.hasKeyword(request.getSearch()))
                .and(ProductSpecifications.hasCategory(request.getCategoryId()));

        Page<Product> productsPage = productRepository.findAll(spec, request.toPageable());

        return mapProductsPageWithPrimaryImages(productsPage);
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
            Brand brand = brandRepository
                    .findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
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

        product = productRepository.save(product);

        return toDetailResponse(product);
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
        if (request.getSkus() == null) {
            return;
        }

        Set<String> skus = new HashSet<>();
        for (ProductSkuCreateRequest skuRequest : request.getSkus()) {
            if (skuRequest.getSku() == null || skuRequest.getSku().isBlank()) {
                throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
            }

            if (!skus.add(skuRequest.getSku()) || productSkuRepository.existsBySku(skuRequest.getSku())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAdminProducts(ProductSearchRequest request) {
        Specification<Product> spec = Specification.where(ProductSpecifications.hasKeyword(request.getSearch()))
                .and(ProductSpecifications.hasCategory(request.getCategoryId()))
                .and(ProductSpecifications.hasStatus(request.getStatus()));

        Page<Product> productsPage = productRepository.findAll(spec, request.toPageable());

        return mapProductsPageWithPrimaryImages(productsPage);
    }

    private Page<ProductResponse> mapProductsPageWithPrimaryImages(Page<Product> productsPage) {
        List<Long> productIds =
                productsPage.getContent().stream().map(Product::getId).toList();
        Map<Long, String> primaryImageUrlsMap = fetchPrimaryImageUrlsMap(productIds);
        return productsPage.map(product -> toSummaryResponse(product, primaryImageUrlsMap.get(product.getId())));
    }

    private Map<Long, String> fetchPrimaryImageUrlsMap(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productImageRepository.findPrimaryImagesByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImageRepository.ProductPrimaryImageProjection::getProductId,
                        proj -> resolveImageUrl(proj.getUrl()),
                        (existing, replacement) -> existing));
    }

    private String resolveImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }
        if (awsS3FileService == null) {
            return rawUrl;
        }
        String resolved = awsS3FileService.resolveAccessUrl(rawUrl);
        return resolved != null ? resolved : rawUrl;
    }

    @Transactional
    public ProductResponse publishProduct(Long id) {
        Product product = findProductById(id);

        long activeSkus =
                product.getSkus().stream().filter(ProductSku::isActive).count();

        if (activeSkus == 0) {
            throw new AppException(ErrorCode.PRODUCT_MUST_HAVE_ACTIVE_SKU);
        }

        product.setDraft(false);
        product.setPublished(true);
        product = productRepository.save(product);
        return toDetailResponse(product);
    }

    @Transactional
    public ProductResponse unpublishProduct(Long id) {
        Product product = findProductById(id);
        product.setDraft(false);
        product.setPublished(false);
        product = productRepository.save(product);
        return toDetailResponse(product);
    }

    public ProductResponse toDetailResponse(Product product) {
        if (product == null) {
            return null;
        }

        ProductResponse response = productMapper.toProductResponse(product);

        if (product.getSkus() != null && !product.getSkus().isEmpty()) {
            response.setSkus(productSkuService.toSkuResponseList(product.getSkus()));
        }

        String primaryRawKey = findPrimaryImageKey(product);
        if (primaryRawKey != null && !primaryRawKey.isBlank()) {
            String resolved = awsS3FileService.resolveAccessUrl(primaryRawKey);
            response.setPrimaryImageUrl(resolved != null ? resolved : primaryRawKey);
        }

        return response;
    }

    public ProductResponse toSummaryResponse(Product product, String resolvedPrimaryImageUrl) {
        if (product == null) {
            return null;
        }

        ProductResponse response = productMapper.toProductResponseSummary(product);
        response.setPrimaryImageUrl(resolvedPrimaryImageUrl);
        return response;
    }

    private String findPrimaryImageKey(Product product) {
        if (product.getSkus() == null) {
            return null;
        }

        return product.getSkus().stream()
                .map(ProductSku::getImages)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(img ->
                        img.isPrimary() && img.getUrl() != null && !img.getUrl().isBlank())
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);
    }
}
