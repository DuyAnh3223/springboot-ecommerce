package spring.abtechzone.modules.product.service;

import java.util.*;

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
import spring.abtechzone.common.service.S3ObjectLifecycleHelper;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.request.SkuPreviewRequest;
import spring.abtechzone.modules.product.dto.response.ProductImageResponse;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.dto.response.SkuPreviewResponse;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductImage;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductImageMapper;
import spring.abtechzone.modules.product.mapper.ProductSkuMapper;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.repository.specification.ProductSkuSpecifications;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuService {

    ProductSkuRepository productSkuRepository;
    ProductRepository productRepository;
    ProductSkuMapper productSkuMapper;
    ProductImageMapper productImageMapper;
    ProductImageRepository productImageRepository;
    AwsS3FileService awsS3FileService;
    S3ObjectLifecycleHelper s3ObjectLifecycleHelper;
    ProductAttributeValidator productAttributeValidator;
    CategoryAttributeRepository categoryAttributeRepository;
    SkuImageService skuImageService;
    SkuVariantPreviewCalculator skuVariantPreviewCalculator = new SkuVariantPreviewCalculator();

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Page<ProductSkuResponse> getSkus(ProductSkuSearchRequest request) {
        Specification<ProductSku> spec = Specification.where(ProductSkuSpecifications.hasKeyword(request.getSearch()))
                .and(ProductSkuSpecifications.hasProductId(request.getProductId()))
                .and(ProductSkuSpecifications.hasMinPrice(request.getMinPrice()))
                .and(ProductSkuSpecifications.hasMaxPrice(request.getMaxPrice()));

        return productSkuRepository.findAll(spec, request.toPageable()).map(this::toSkuResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductSkuResponse getSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));
        return toSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse createSku(ProductSkuCreateRequest request) {
        if (request.getProductId() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductSku sku = createSingleSkuInternal(product, request, product.getSkus());
        return toSkuResponse(sku);
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

        sku = productSkuRepository.save(sku);

        if (request.getImages() != null) {
            skuImageService.syncSkuImages(sku, request.getImages());
        }

        return toSkuResponse(sku);
    }

    @Transactional
    public void deleteSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        Product product = sku.getProduct();
        if (product.isPublished()) {
            long remainingActive = product.getSkus().stream()
                    .filter(s -> !s.getId().equals(skuId) && s.isActive())
                    .count();
            if (remainingActive == 0) {
                throw new AppException(ErrorCode.PRODUCT_MUST_HAVE_ACTIVE_SKU);
            }
        }

        List<ProductImage> galleryImages = productImageRepository.findBySkuIdOrderBySortOrderAsc(skuId);
        for (ProductImage img : galleryImages) {
            s3ObjectLifecycleHelper.deleteAfterCommit(img.getUrl());
        }
        productImageRepository.deleteBySkuId(skuId);

        sku.setImageUrl(null);
        sku.softDelete();
        productSkuRepository.save(sku);
    }

    @PreAuthorize("permitAll()")
    public ProductSkuResponse toSkuResponse(ProductSku sku) {
        if (sku == null) {
            return null;
        }

        ProductSkuResponse response = productSkuMapper.toProductSkuResponse(sku);

        String rawImageUrl = sku.getImageUrl();
        if ((rawImageUrl == null || rawImageUrl.isBlank())
                && sku.getImages() != null
                && !sku.getImages().isEmpty()) {
            rawImageUrl = sku.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getUrl)
                    .orElse(sku.getImages().getFirst().getUrl());
        }

        if (rawImageUrl != null && !rawImageUrl.isBlank()) {
            String resolved = awsS3FileService.resolveAccessUrl(rawImageUrl);
            response.setImageUrl(resolved != null ? resolved : rawImageUrl);
        }

        if (sku.getImages() != null && !sku.getImages().isEmpty()) {
            List<ProductImageResponse> imageResponses =
                    sku.getImages().stream().map(this::toProductImageResponse).toList();
            response.setImages(imageResponses);
        }

        return response;
    }

    @PreAuthorize("permitAll()")
    public List<ProductSkuResponse> toSkuResponseList(List<ProductSku> skus) {
        if (skus == null) {
            return Collections.emptyList();
        }
        return skus.stream().map(this::toSkuResponse).toList();
    }

    private ProductImageResponse toProductImageResponse(ProductImage image) {
        if (image == null) {
            return null;
        }
        ProductImageResponse response = productImageMapper.toProductImageResponse(image);
        if (image.getUrl() != null && !image.getUrl().isBlank()) {
            String resolved = awsS3FileService.resolveAccessUrl(image.getUrl());
            response.setAccessUrl(resolved != null ? resolved : image.getUrl());
        }
        return response;
    }

    private ProductSku createSingleSkuInternal(
            Product product, ProductSkuCreateRequest request, List<ProductSku> currentSkus) {
        validateSkuForCreate(request.getSku());

        ProductSku sku = productSkuMapper.toProductSku(request);
        sku.setProduct(product);
        productAttributeValidator.validateSkuAttributes(product, sku.getAttributes());
        productAttributeValidator.validateSkuNotDuplicate(product, currentSkus, sku.getAttributes());

        sku = productSkuRepository.save(sku);

        if (request.getImages() != null) {
            skuImageService.syncSkuImages(sku, request.getImages());
        }

        return sku;
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

    @Transactional(readOnly = true)
    public List<SkuPreviewResponse> previewSkus(Long productId, SkuPreviewRequest request) {
        Product product =
                productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Long categoryId = product.getCategory().getId();
        List<CategoryAttribute> variantDefs =
                categoryAttributeRepository.findByCategoryIdWithAttribute(categoryId).stream()
                        .filter(ca -> Boolean.TRUE.equals(ca.getIsVariantDefining()))
                        .toList();

        Map<String, List<Object>> inputAttrs =
                request.getAttributes() == null ? Collections.emptyMap() : request.getAttributes();

        return skuVariantPreviewCalculator.calculatePreview(variantDefs, inputAttrs);
    }

    @Transactional
    public List<ProductSkuResponse> createSkusBulk(Long productId, List<ProductSkuCreateRequest> requests) {
        Product product =
                productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductSku> currentSkus = new ArrayList<>(product.getSkus());
        List<ProductSkuResponse> savedResponses = new ArrayList<>();

        for (ProductSkuCreateRequest skuRequest : requests) {
            ProductSku sku = createSingleSkuInternal(product, skuRequest, currentSkus);
            currentSkus.add(sku);
            savedResponses.add(toSkuResponse(sku));
        }
        return savedResponses;
    }
}
