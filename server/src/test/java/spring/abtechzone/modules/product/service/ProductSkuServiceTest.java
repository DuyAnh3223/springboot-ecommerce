package spring.abtechzone.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.product.dto.request.ProductSkuItemRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuReconcileRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductImageMapper;
import spring.abtechzone.modules.product.mapper.ProductSkuMapper;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@ExtendWith(MockitoExtension.class)
class ProductSkuServiceTest {

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductSkuMapper productSkuMapper;

    @Mock
    ProductImageMapper productImageMapper;

    @Mock
    AwsS3FileService awsS3FileService;

    @Mock
    ProductAttributeValidator productAttributeValidator;

    @Mock
    SkuImageService skuImageService;

    @Mock
    InventoryService inventoryService;

    @InjectMocks
    ProductSkuService productSkuService;

    Product sampleProduct;
    ProductSku sampleSku;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder().id(100L).name("Test Product").build();
        sampleSku = ProductSku.builder()
                .id(10L)
                .product(sampleProduct)
                .sku("SKU-100-RED")
                .price(BigDecimal.valueOf(100000))
                .imageUrl("products/10/old-primary.png")
                .build();
        lenient().when(inventoryService.getOnHandOrZero(any())).thenReturn(10);
        lenient().when(inventoryService.getOnHandBySkuIds(any())).thenReturn(java.util.Map.of(10L, 10));
        lenient().when(inventoryService.getTotalOnHandByProductIds(any())).thenReturn(java.util.Map.of(100L, 10));
    }

    @Test
    @DisplayName("updateSku with images field absent (null) should preserve existing gallery")
    void updateSku_ImagesAbsent_ShouldPreserveGallery() {
        ProductSkuUpdateRequest request = ProductSkuUpdateRequest.builder()
                .price(BigDecimal.valueOf(120000))
                .images(null)
                .build();

        when(productSkuRepository.findById(10L)).thenReturn(Optional.of(sampleSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(i -> i.getArgument(0));
        when(productSkuMapper.toProductSkuResponse(any()))
                .thenReturn(
                        ProductSkuResponse.builder().id(10L).sku("SKU-100-RED").build());

        ProductSkuResponse response = productSkuService.updateSku(10L, request);

        assertThat(response).isNotNull();
        verify(skuImageService, never()).syncSkuImages(any(), any());
    }

    @Test
    @DisplayName("reconcileSkus with valid request should lock product and reconcile SKUs")
    void reconcileSkus_ValidRequest_ShouldLockProductAndReconcile() {
        ProductSkuReconcileRequest request = ProductSkuReconcileRequest.builder()
                .removedSkuIds(java.util.List.of(20L))
                .skus(java.util.List.of(ProductSkuItemRequest.builder()
                        .id(10L)
                        .sku("SKU-100-RED")
                        .price(BigDecimal.valueOf(150000))
                        .stock(20)
                        .build()))
                .build();

        ProductSku removeSku =
                ProductSku.builder().id(20L).sku("SKU-100-BLUE").active(true).build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleProduct));
        when(productSkuRepository.findByProductIdAndDeletedAtIsNull(100L))
                .thenReturn(java.util.List.of(sampleSku, removeSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(i -> i.getArgument(0));

        productSkuService.reconcileSkus(100L, request);

        verify(productRepository).findByIdForUpdate(100L);
        assertThat(removeSku.isActive()).isFalse();
        assertThat(removeSku.getDeletedAt()).isNotNull();
        assertThat(sampleSku.getPrice()).isEqualTo(BigDecimal.valueOf(150000));
        verify(inventoryService).setOnHand(10L, 20);
    }

    @Test
    @DisplayName("reconcileSkus with overlapping removed and upsert ID should throw exception")
    void reconcileSkus_OverlappingRemovedAndUpsertId_ShouldThrowException() {
        ProductSkuReconcileRequest request = ProductSkuReconcileRequest.builder()
                .removedSkuIds(java.util.List.of(10L))
                .skus(java.util.List.of(ProductSkuItemRequest.builder()
                        .id(10L)
                        .sku("SKU-100-RED")
                        .price(BigDecimal.valueOf(150000))
                        .stock(20)
                        .build()))
                .build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleProduct));
        when(productSkuRepository.findByProductIdAndDeletedAtIsNull(100L)).thenReturn(java.util.List.of(sampleSku));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> productSkuService.reconcileSkus(100L, request))
                .isInstanceOf(spring.abtechzone.common.exception.AppException.class);
    }

    @Test
    @DisplayName("reconcileSkus with foreign removedSkuId not belonging to product should throw SKU_NOT_FOUND")
    void reconcileSkus_InvalidRemovedSkuIdScope_ShouldThrowException() {
        ProductSkuReconcileRequest request = ProductSkuReconcileRequest.builder()
                .removedSkuIds(java.util.List.of(999L)) // Foreign SKU ID
                .skus(java.util.List.of())
                .build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleProduct));
        when(productSkuRepository.findByProductIdAndDeletedAtIsNull(100L)).thenReturn(java.util.List.of(sampleSku));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> productSkuService.reconcileSkus(100L, request))
                .isInstanceOf(spring.abtechzone.common.exception.AppException.class);
    }

    @Test
    @DisplayName("reconcileSkus should calculate price min/max aggregates correctly using Object[] bounds")
    void reconcileSkus_PriceMinMaxBounds_ShouldCalculateAggregatesCorrectly() {
        ProductSkuReconcileRequest request = ProductSkuReconcileRequest.builder()
                .skus(java.util.List.of(ProductSkuItemRequest.builder()
                        .id(10L)
                        .sku("SKU-100-RED")
                        .price(BigDecimal.valueOf(100000))
                        .stock(10)
                        .build()))
                .build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleProduct));
        when(productSkuRepository.findByProductIdAndDeletedAtIsNull(100L)).thenReturn(java.util.List.of(sampleSku));
        when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(i -> i.getArgument(0));
        when(productSkuRepository.countByProductIdAndDeletedAtIsNull(100L)).thenReturn(1L);
        when(productSkuRepository.countByProductIdAndDeletedAtIsNullAndActiveTrue(100L))
                .thenReturn(1L);
        when(productSkuRepository.findPriceMinAndMaxByProductIdAndActiveTrue(100L))
                .thenReturn(new Object[] {BigDecimal.valueOf(100000), BigDecimal.valueOf(250000)});

        productSkuService.reconcileSkus(100L, request);

        assertThat(sampleProduct.getPriceMin()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(sampleProduct.getPriceMax()).isEqualTo(BigDecimal.valueOf(250000));
    }

    @Test
    @DisplayName("reconcileSkus with duplicate variant combination should throw AppException")
    void reconcileSkus_DuplicateVariantCombination_ShouldThrowException() {
        ProductSkuReconcileRequest request = ProductSkuReconcileRequest.builder()
                .skus(java.util.List.of(
                        ProductSkuItemRequest.builder()
                                .sku("SKU-RED-16GB-1")
                                .price(BigDecimal.valueOf(100000))
                                .stock(10)
                                .attributes(java.util.Map.of("color", "red", "ram", "16gb"))
                                .build(),
                        ProductSkuItemRequest.builder()
                                .sku("SKU-RED-16GB-2")
                                .price(BigDecimal.valueOf(110000))
                                .stock(5)
                                .attributes(java.util.Map.of("color", "red", "ram", "16gb"))
                                .build()))
                .build();

        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(sampleProduct));
        when(productSkuRepository.findByProductIdAndDeletedAtIsNull(100L)).thenReturn(java.util.List.of());

        doThrow(new spring.abtechzone.common.exception.AppException(
                        spring.abtechzone.common.exception.ErrorCode.PRODUCT_SKU_ATTRIBUTES_DUPLICATED))
                .when(productAttributeValidator)
                .validateSkuNotDuplicate(any(), any(), eq(java.util.Map.of("color", "red", "ram", "16gb")));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> productSkuService.reconcileSkus(100L, request))
                .isInstanceOf(spring.abtechzone.common.exception.AppException.class);
    }
}
