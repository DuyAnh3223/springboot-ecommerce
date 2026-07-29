package spring.abtechzone.modules.product.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.common.service.S3ObjectLifecycleHelper;
import spring.abtechzone.modules.product.dto.request.ProductImageRequest;
import spring.abtechzone.modules.product.entity.ProductImage;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkuImageService {

    ProductImageRepository productImageRepository;
    ProductSkuRepository productSkuRepository;
    AwsS3FileService awsS3FileService;
    S3ObjectLifecycleHelper s3ObjectLifecycleHelper;

    public List<ProductImage> syncSkuImages(ProductSku sku, List<ProductImageRequest> imageRequests) {
        if (imageRequests == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        List<ProductImage> existingImages = productImageRepository.findBySkuIdOrderBySortOrderAsc(sku.getId());

        if (imageRequests.isEmpty()) {
            return clearAllSkuImages(sku, existingImages);
        }

        validateImageRequests(imageRequests);

        Map<Long, ProductImage> existingMap =
                existingImages.stream().collect(Collectors.toMap(ProductImage::getId, Function.identity()));

        Set<Long> retainedIds = new HashSet<>();
        List<ProductImage> existingToSave = new ArrayList<>();
        List<ProductImage> newToSave = new ArrayList<>();

        for (ProductImageRequest req : imageRequests) {
            if (req.getId() != null) {
                ProductImage img = processExistingImage(req, existingMap, sku);
                existingToSave.add(img);
                retainedIds.add(img.getId());
            } else {
                ProductImage img = createNewImage(req, sku);
                newToSave.add(img);
            }
        }

        // 1. Delete removed images and flush to DB
        deleteRemovedImages(existingImages, retainedIds);
        productImageRepository.flush();

        // 2. Clear primary flag on non-primary existing images and flush
        for (ProductImage img : existingToSave) {
            if (!img.isPrimary()) {
                productImageRepository.save(img);
            }
        }
        productImageRepository.flush();

        // 3. Save remaining existing images (including new primary) and new images
        List<ProductImage> savedImages = new ArrayList<>();
        savedImages.addAll(productImageRepository.saveAll(existingToSave));
        savedImages.addAll(productImageRepository.saveAll(newToSave));

        updateSkuState(sku, savedImages);

        return savedImages;
    }

    private void validateImageRequests(List<ProductImageRequest> imageRequests) {
        if (imageRequests.size() > 10) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Set<Long> requestIds = new HashSet<>();
        for (ProductImageRequest req : imageRequests) {
            if (req.getId() != null && !requestIds.add(req.getId())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        long primaryCount =
                imageRequests.stream().filter(ProductImageRequest::isPrimary).count();
        if (primaryCount != 1) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private List<ProductImage> clearAllSkuImages(ProductSku sku, List<ProductImage> existingImages) {
        for (ProductImage img : existingImages) {
            if (img.getUrl() != null) {
                s3ObjectLifecycleHelper.deleteAfterCommit(img.getUrl());
            }
        }
        productImageRepository.deleteBySkuId(sku.getId());
        if (sku.getImages() != null) {
            sku.getImages().clear();
        }
        sku.setImageUrl(null);
        productSkuRepository.save(sku);
        return Collections.emptyList();
    }

    private ProductImage processExistingImage(
            ProductImageRequest req, Map<Long, ProductImage> existingMap, ProductSku sku) {
        ProductImage existing = existingMap.get(req.getId());
        if (existing == null || !existing.getSku().getId().equals(sku.getId())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (req.getUrl() != null && !req.getUrl().isBlank()) {
            String extractedKey = extractKey(req.getUrl());
            if (extractedKey == null || !extractedKey.equals(existing.getUrl())) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
        }

        existing.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        existing.setPrimary(req.isPrimary());
        return existing;
    }

    private ProductImage createNewImage(ProductImageRequest req, ProductSku sku) {
        String extractedKey = extractKey(req.getUrl());
        if (extractedKey == null || extractedKey.isBlank()) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        return ProductImage.builder()
                .product(sku.getProduct())
                .sku(sku)
                .url(extractedKey)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .primary(req.isPrimary())
                .build();
    }

    private String extractKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String key = awsS3FileService.extractS3Key(url);
        return (key == null || key.isBlank()) ? url : key;
    }

    private void deleteRemovedImages(List<ProductImage> existingImages, Set<Long> retainedIds) {
        for (ProductImage oldImg : existingImages) {
            if (!retainedIds.contains(oldImg.getId())) {
                productImageRepository.delete(oldImg);
                s3ObjectLifecycleHelper.deleteAfterCommit(oldImg.getUrl());
            }
        }
    }

    private void updateSkuState(ProductSku sku, List<ProductImage> savedImages) {
        if (sku.getImages() != null) {
            sku.getImages().clear();
            sku.getImages().addAll(savedImages);
        }

        savedImages.stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .ifPresent(primaryImg -> sku.setImageUrl(primaryImg.getUrl()));

        productSkuRepository.save(sku);
    }
}
