package spring.abtechzone.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.common.service.S3ObjectLifecycleHelper;
import spring.abtechzone.modules.product.dto.request.ProductImageRequest;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductImage;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;

@ExtendWith(MockitoExtension.class)
class SkuImageServiceTest {

    @Mock
    ProductImageRepository productImageRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    AwsS3FileService awsS3FileService;

    @Mock
    S3ObjectLifecycleHelper s3ObjectLifecycleHelper;

    @InjectMocks
    SkuImageService skuImageService;

    Product sampleProduct;
    ProductSku sampleSku;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder().id(100L).name("Test Product").build();
        sampleSku = ProductSku.builder()
                .id(10L)
                .product(sampleProduct)
                .sku("SKU-100-RED")
                .imageUrl("products/10/old-primary.png")
                .build();
    }

    @Test
    @DisplayName("syncSkuImages with null imageRequests throws INVALID_KEY")
    void syncSkuImages_NullRequests_ThrowsException() {
        assertThatThrownBy(() -> skuImageService.syncSkuImages(sampleSku, null)).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("syncSkuImages with empty list deletes gallery images and clears imageUrl")
    void syncSkuImages_EmptyList_DeletesGalleryAndClearsUrl() {
        ProductImage oldImage = ProductImage.builder()
                .id(1L)
                .sku(sampleSku)
                .url("products/10/old-primary.png")
                .primary(true)
                .build();

        when(productImageRepository.findBySkuIdOrderBySortOrderAsc(10L)).thenReturn(List.of(oldImage));

        List<ProductImage> result = skuImageService.syncSkuImages(sampleSku, List.of());

        assertThat(result).isEmpty();
        verify(s3ObjectLifecycleHelper).deleteAfterCommit("products/10/old-primary.png");
        verify(productImageRepository).deleteBySkuId(10L);
        verify(productSkuRepository).save(sampleSku);
        assertThat(sampleSku.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("syncSkuImages with image ID belonging to another SKU throws exception")
    void syncSkuImages_ImageBelongsToOtherSku_ThrowsException() {
        ProductSku otherSku = ProductSku.builder().id(99L).build();
        ProductImage otherSkuImage = ProductImage.builder()
                .id(50L)
                .sku(otherSku)
                .url("products/99/image.png")
                .primary(true)
                .build();

        when(productImageRepository.findBySkuIdOrderBySortOrderAsc(10L)).thenReturn(List.of(otherSkuImage));

        ProductImageRequest req = ProductImageRequest.builder()
                .id(50L)
                .url("products/99/image.png")
                .primary(true)
                .build();

        assertThatThrownBy(() -> skuImageService.syncSkuImages(sampleSku, List.of(req)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("syncSkuImages with duplicate IDs throws exception")
    void syncSkuImages_DuplicateId_ThrowsException() {
        ProductImage existing = ProductImage.builder()
                .id(1L)
                .sku(sampleSku)
                .url("products/10/img.png")
                .primary(true)
                .build();

        when(productImageRepository.findBySkuIdOrderBySortOrderAsc(10L)).thenReturn(List.of(existing));

        ProductImageRequest req1 = ProductImageRequest.builder()
                .id(1L)
                .url("products/10/img.png")
                .primary(true)
                .build();
        ProductImageRequest req2 = ProductImageRequest.builder()
                .id(1L)
                .url("products/10/img.png")
                .primary(false)
                .build();

        assertThatThrownBy(() -> skuImageService.syncSkuImages(sampleSku, List.of(req1, req2)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("syncSkuImages with 0 primary images throws exception")
    void syncSkuImages_NoPrimary_ThrowsException() {
        ProductImageRequest req = ProductImageRequest.builder()
                .url("products/10/img.png")
                .primary(false)
                .build();

        assertThatThrownBy(() -> skuImageService.syncSkuImages(sampleSku, List.of(req)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("syncSkuImages with changed URL for existing image ID throws exception")
    void syncSkuImages_UrlMismatch_ThrowsException() {
        ProductImage existingImage = ProductImage.builder()
                .id(1L)
                .sku(sampleSku)
                .url("products/10/original.png")
                .primary(true)
                .build();

        when(productImageRepository.findBySkuIdOrderBySortOrderAsc(10L)).thenReturn(List.of(existingImage));
        when(awsS3FileService.extractS3Key("https://cdn.com/products/10/new-url.png"))
                .thenReturn("products/10/new-url.png");

        ProductImageRequest req = ProductImageRequest.builder()
                .id(1L)
                .url("https://cdn.com/products/10/new-url.png")
                .primary(true)
                .build();

        assertThatThrownBy(() -> skuImageService.syncSkuImages(sampleSku, List.of(req)))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("Replace image scenario: omit old ID and add new item without ID")
    void syncSkuImages_ReplaceImage_DeletesOldAndSavesNew() {
        ProductImage oldImage = ProductImage.builder()
                .id(1L)
                .sku(sampleSku)
                .url("products/10/old-image.png")
                .primary(true)
                .build();

        when(productImageRepository.findBySkuIdOrderBySortOrderAsc(10L)).thenReturn(List.of(oldImage));
        when(awsS3FileService.extractS3Key("https://cdn.com/products/10/new-image.png"))
                .thenReturn("products/10/new-image.png");
        when(productImageRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        ProductImageRequest newReq = ProductImageRequest.builder()
                .url("https://cdn.com/products/10/new-image.png")
                .primary(true)
                .sortOrder(0)
                .build();

        List<ProductImage> result = skuImageService.syncSkuImages(sampleSku, List.of(newReq));

        assertThat(result).hasSize(1);
        verify(productImageRepository).delete(oldImage);
        verify(s3ObjectLifecycleHelper).deleteAfterCommit("products/10/old-image.png");
        assertThat(sampleSku.getImageUrl()).isEqualTo("products/10/new-image.png");
    }
}
