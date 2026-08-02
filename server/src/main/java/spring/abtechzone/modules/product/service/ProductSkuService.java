package spring.abtechzone.modules.product.service;

import java.math.BigDecimal;
import java.util.*;
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
import spring.abtechzone.common.service.S3ObjectLifecycleHelper;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.product.dto.request.*;
import spring.abtechzone.modules.product.dto.request.ProductSkuItemRequest;
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

    @Transactional
    public void reconcileSkus(Long productId, ProductSkuReconcileRequest request) {
        Product product = productRepository
                .findByIdForUpdate(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductSku> existingSkus = productSkuRepository.findByProductIdAndDeletedAtIsNull(productId);
        Set<Long> existingSkuIds = existingSkus.stream().map(ProductSku::getId).collect(Collectors.toSet());
        Set<Long> removedSet =
                request.getRemovedSkuIds() != null ? new HashSet<>(request.getRemovedSkuIds()) : Collections.emptySet();
        List<ProductSkuItemRequest> upsertItems =
                request.getSkus() != null ? request.getSkus() : Collections.emptyList();

        validateReconcileScope(existingSkuIds, removedSet, upsertItems);
        processRemovedSkus(product, existingSkus, removedSet, upsertItems);
        processUpsertSkus(product, existingSkus, removedSet, upsertItems);

        recalculateProductAggregates(product);
        productRepository.save(product);
    }

    private void validateReconcileScope(
            Set<Long> existingSkuIds, Set<Long> removedSet, List<ProductSkuItemRequest> upsertItems) {
        for (Long removeId : removedSet) {
            if (!existingSkuIds.contains(removeId)) {
                throw new AppException(ErrorCode.SKU_NOT_FOUND);
            }
        }
        for (ProductSkuItemRequest item : upsertItems) {
            if (item.getId() != null && removedSet.contains(item.getId())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
            }
        }
    }

    private void processRemovedSkus(
            Product product,
            List<ProductSku> existingSkus,
            Set<Long> removedSet,
            List<ProductSkuItemRequest> upsertItems) {
        if (removedSet.isEmpty()) {
            return;
        }

        long activeExistingRemaining = existingSkus.stream()
                .filter(s -> s.isActive() && s.getDeletedAt() == null && !removedSet.contains(s.getId()))
                .count();
        long newSkusCount = upsertItems.stream().filter(r -> r.getId() == null).count();

        if (product.isPublished() && (activeExistingRemaining + newSkusCount == 0)) {
            throw new AppException(ErrorCode.PRODUCT_MUST_HAVE_ACTIVE_SKU);
        }

        for (Long removeId : removedSet) {
            existingSkus.stream()
                    .filter(s -> s.getId().equals(removeId) && s.getDeletedAt() == null)
                    .findFirst()
                    .ifPresent(skuToRemove -> {
                        skuToRemove.softDelete();
                        productSkuRepository.save(skuToRemove);
                    });
        }
    }

    private void processUpsertSkus(
            Product product,
            List<ProductSku> existingSkus,
            Set<Long> removedSet,
            List<ProductSkuItemRequest> upsertItems) {
        if (upsertItems.isEmpty()) {
            return;
        }

        validateRequestSkuCodesUnique(upsertItems);

        List<ProductSku> workingActiveSkus = new ArrayList<>(existingSkus.stream()
                .filter(s -> !removedSet.contains(s.getId()) && s.getDeletedAt() == null)
                .toList());

        for (ProductSkuItemRequest item : upsertItems) {
            if (item.getId() != null) {
                updateSingleSku(product, existingSkus, item, workingActiveSkus);
            } else {
                createSingleSku(product, item, workingActiveSkus);
            }
        }
    }

    private void validateRequestSkuCodesUnique(List<ProductSkuItemRequest> upsertItems) {
        Set<String> requestSkuCodes = new HashSet<>();
        for (ProductSkuItemRequest item : upsertItems) {
            if (item.getSku() == null || item.getSku().isBlank()) {
                throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
            }
            if (!requestSkuCodes.add(item.getSku())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
        }
    }

    private void updateSingleSku(
            Product product,
            List<ProductSku> existingSkus,
            ProductSkuItemRequest item,
            List<ProductSku> workingActiveSkus) {
        ProductSku sku = existingSkus.stream()
                .filter(s -> s.getId().equals(item.getId()) && s.getDeletedAt() == null)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        validateSkuForUpdate(sku.getId(), item.getSku());

        Map<String, Object> updatedAttrs = item.getAttributes() != null ? item.getAttributes() : sku.getAttributes();
        productAttributeValidator.validateSkuAttributes(product, updatedAttrs);

        Long targetId = sku.getId();
        List<ProductSku> otherSkus = workingActiveSkus.stream()
                .filter(s -> !s.getId().equals(targetId))
                .toList();
        productAttributeValidator.validateSkuNotDuplicate(product, otherSkus, updatedAttrs);

        sku.setSku(item.getSku());
        sku.setPrice(item.getPrice());
        sku.setStock(item.getStock());
        sku.setWeightGram(item.getWeightGram());
        if (item.getCurrency() != null) sku.setCurrency(item.getCurrency());
        sku.setAttributes(updatedAttrs);
        sku.setActive(true);

        sku = productSkuRepository.save(sku);

        if (item.getImages() != null) {
            skuImageService.syncSkuImages(sku, item.getImages());
        }

        Long updatedId = sku.getId();
        for (int i = 0; i < workingActiveSkus.size(); i++) {
            if (workingActiveSkus.get(i).getId().equals(updatedId)) {
                workingActiveSkus.set(i, sku);
                break;
            }
        }
    }

    private void createSingleSku(Product product, ProductSkuItemRequest item, List<ProductSku> workingActiveSkus) {
        validateSkuForCreate(item.getSku());

        Map<String, Object> attrs = item.getAttributes() != null ? item.getAttributes() : Map.of();
        productAttributeValidator.validateSkuAttributes(product, attrs);
        productAttributeValidator.validateSkuNotDuplicate(product, workingActiveSkus, attrs);

        ProductSku newSku = ProductSku.builder()
                .product(product)
                .sku(item.getSku())
                .price(item.getPrice())
                .stock(item.getStock())
                .weightGram(item.getWeightGram())
                .currency(item.getCurrency() != null ? item.getCurrency() : "VND")
                .attributes(attrs)
                .active(true)
                .build();

        newSku = productSkuRepository.save(newSku);

        if (item.getImages() != null) {
            skuImageService.syncSkuImages(newSku, item.getImages());
        }

        workingActiveSkus.add(newSku);
    }

    private void recalculateProductAggregates(Product product) {
        Long productId = product.getId();
        int totalSkuCount = (int) productSkuRepository.countByProductIdAndDeletedAtIsNull(productId);
        int activeSkuCount = (int) productSkuRepository.countByProductIdAndDeletedAtIsNullAndActiveTrue(productId);
        int totalStock = productSkuRepository.sumStockByProductIdAndActiveTrue(productId);

        product.setSkuCount(totalSkuCount);
        product.setActiveSkuCount(activeSkuCount);
        product.setTotalStock(totalStock);

        if (activeSkuCount > 0) {
            Object[] bounds = productSkuRepository.findPriceMinAndMaxByProductIdAndActiveTrue(productId);
            if (bounds != null && bounds.length >= 2 && bounds[0] != null) {
                product.setPriceMin((BigDecimal) bounds[0]);
                product.setPriceMax((BigDecimal) bounds[1]);
            } else {
                product.setPriceMin(BigDecimal.ZERO);
                product.setPriceMax(BigDecimal.ZERO);
            }
        } else {
            product.setPriceMin(BigDecimal.ZERO);
            product.setPriceMax(BigDecimal.ZERO);
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
