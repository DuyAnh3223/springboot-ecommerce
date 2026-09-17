package spring.abtechzone.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.common.service.S3ObjectLifecycleHelper;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.mapper.BrandMapper;
import spring.abtechzone.modules.category.repository.BrandRepository;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.product.dto.request.ProductImageRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuItemRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuReconcileRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductImageMapper;
import spring.abtechzone.modules.product.mapper.ProductMapper;
import spring.abtechzone.modules.product.mapper.ProductSkuMapper;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@ExtendWith(MockitoExtension.class)
class LegacyProductUpdateTest {
    private static final Long PRODUCT_ID = 114L;
    private static final Long SKU_ID = 114L;
    private static final Map<String, Object> LEGACY_ATTRIBUTES = Map.of("color", "Standard");
    private static final BigDecimal UPDATED_PRICE = BigDecimal.valueOf(1500000);

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    ProductImageRepository productImageRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    BrandRepository brandRepository;

    @Mock
    CategoryAttributeRepository categoryAttributeRepository;

    @Mock
    AwsS3FileService awsS3FileService;

    @Mock
    S3ObjectLifecycleHelper s3ObjectLifecycleHelper;

    @Mock
    SkuImageService skuImageService;

    @Mock
    InventoryService inventoryService;

    ProductService productService;
    ProductSkuService productSkuService;
    Product product;
    ProductSku sku;
    CategoryAttribute coreCountDefinition;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(1L);
        category.setName("CPU");
        coreCountDefinition = definition("core_count", "NUMBER", false);
        coreCountDefinition.setIsRequired(true);
        product = Product.builder()
                .id(PRODUCT_ID)
                .name("AMD Ryzen 5 3600")
                .category(category)
                .attributes(new HashMap<>(Map.of("core_count", 6)))
                .build();
        sku = ProductSku.builder()
                .id(SKU_ID)
                .product(product)
                .sku("CPU-RYZEN-3600")
                .price(BigDecimal.valueOf(1000000))
                .attributes(new HashMap<>(LEGACY_ATTRIBUTES))
                .images(new ArrayList<>())
                .active(true)
                .build();
        product.setSkus(new ArrayList<>(List.of(sku)));

        ProductImageMapper imageMapper = Mappers.getMapper(ProductImageMapper.class);
        ProductSkuMapper skuMapper = Mappers.getMapper(ProductSkuMapper.class);
        ReflectionTestUtils.setField(skuMapper, "productImageMapper", imageMapper);
        ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
        ReflectionTestUtils.setField(productMapper, "productSkuMapper", skuMapper);
        ReflectionTestUtils.setField(productMapper, "brandMapper", Mappers.getMapper(BrandMapper.class));
        ProductAttributeValidator validator = new ProductAttributeValidator(categoryAttributeRepository);
        productSkuService = new ProductSkuService(
                productSkuRepository,
                productRepository,
                skuMapper,
                imageMapper,
                productImageRepository,
                awsS3FileService,
                s3ObjectLifecycleHelper,
                validator,
                categoryAttributeRepository,
                skuImageService,
                inventoryService);
        productService = new ProductService(
                productRepository,
                productSkuRepository,
                productImageRepository,
                productMapper,
                productSkuService,
                validator,
                categoryRepository,
                brandRepository,
                awsS3FileService,
                inventoryService);

        // Shared fixtures serve three entry points and branches that deliberately skip validation.
        lenient()
                .when(categoryAttributeRepository.findByCategoryIdWithAttribute(1L))
                .thenReturn(List.of(coreCountDefinition));
        lenient().when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        lenient().when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
        lenient().when(productSkuRepository.findById(SKU_ID)).thenReturn(Optional.of(sku));
        lenient()
                .when(productSkuRepository.findByProductIdAndDeletedAtIsNull(PRODUCT_ID))
                .thenReturn(product.getSkus());
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(productSkuRepository.save(any(ProductSku.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void productMetadataUpdatePreservesLegacySkuAttributes() {
        var response = productService.update(
                PRODUCT_ID,
                ProductUpdateRequest.builder()
                        .description("Updated CPU details")
                        .build());

        assertThat(response.getDescription()).isEqualTo("Updated CPU details");
        assertThat(product.getAttributes()).containsExactlyEntriesOf(Map.of("core_count", 6));
        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
        assertThat(sku.getImages()).isEmpty();
        verify(productRepository).save(product);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void productMetadataUpdatePreservesOmittedOrEchoedLegacyProductAttributes(boolean echoAttributes) {
        product.setAttributes(new HashMap<>(Map.of("legacy_socket", "AM4")));
        Map<String, Object> attributes = echoAttributes ? new HashMap<>(product.getAttributes()) : null;

        var response = productService.update(
                PRODUCT_ID,
                ProductUpdateRequest.builder()
                        .description("Updated details")
                        .attributes(attributes)
                        .build());

        assertThat(response.getDescription()).isEqualTo("Updated details");
        assertThat(product.getAttributes()).containsExactlyEntriesOf(Map.of("legacy_socket", "AM4"));
        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
    }

    @Test
    void changedValidProductAttributesDoNotRevalidateUnchangedLegacySku() {
        var response = productService.update(
                PRODUCT_ID,
                ProductUpdateRequest.builder()
                        .attributes(Map.of("core_count", 8))
                        .build());

        assertThat(response.getAttributes()).containsExactlyEntriesOf(Map.of("core_count", 8));
        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
        verify(productRepository).save(product);
    }

    @Test
    void changedInvalidProductAttributeTypeIsRejected() {
        assertThatThrownBy(() -> productService.update(
                        PRODUCT_ID,
                        ProductUpdateRequest.builder()
                                .attributes(Map.of("core_count", "eight"))
                                .build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);

        verify(productRepository, never()).save(any());
    }

    @Test
    void clearingRequiredProductAttributesIsRejected() {
        assertThatThrownBy(() -> productService.update(
                        PRODUCT_ID,
                        ProductUpdateRequest.builder().attributes(Map.of()).build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ATTRIBUTES_REQUIRED);

        verify(productRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void skuImageAndPriceUpdatePreservesOmittedOrEchoedLegacyAttributes(boolean echoAttributes) {
        List<ProductImageRequest> images = firstImage();
        var request = ProductSkuUpdateRequest.builder()
                .price(UPDATED_PRICE)
                .attributes(echoAttributes ? new HashMap<>(LEGACY_ATTRIBUTES) : null)
                .images(images)
                .build();

        var response = productSkuService.updateSku(SKU_ID, request);

        assertThat(response.getPrice()).isEqualByComparingTo(UPDATED_PRICE);
        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
        verify(productSkuRepository).save(sku);
        verify(skuImageService).syncSkuImages(sku, images);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reconcileImageAndPriceUpdatePreservesOmittedOrEchoedLegacyAttributes(boolean echoAttributes) {
        List<ProductImageRequest> images = firstImage();
        var item = existingItem(echoAttributes ? new HashMap<>(LEGACY_ATTRIBUTES) : null);
        item.setImages(images);

        reconcile(item);

        assertThat(sku.getPrice()).isEqualByComparingTo(UPDATED_PRICE);
        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
        verify(productSkuRepository).save(sku);
        verify(skuImageService).syncSkuImages(sku, images);
        verify(inventoryService).setOnHand(SKU_ID, 3);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changedInvalidSkuAttributesAreRejectedAcrossUpdatePaths(boolean reconcilePath) {
        assertThatThrownBy(() -> updateAttributes(reconcilePath, Map.of("color", "Red")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);

        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
        verify(productSkuRepository, never()).save(any());
        verify(skuImageService, never()).syncSkuImages(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void validSkuAttributeCleanupIsAllowedAcrossUpdatePaths(boolean reconcilePath) {
        updateAttributes(reconcilePath, Map.of());

        assertThat(sku.getAttributes()).isEmpty();
        assertThat(sku.getPrice()).isEqualByComparingTo(UPDATED_PRICE);
        verify(productSkuRepository).save(sku);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newSkuStillRejectsLegacyAttributeShape(boolean reconcilePath) {
        assertThatThrownBy(() -> {
                    if (reconcilePath) {
                        var item = existingItem(LEGACY_ATTRIBUTES);
                        item.setId(null);
                        item.setSku("CPU-NEW");
                        reconcile(item);
                    } else {
                        productSkuService.createSku(ProductSkuCreateRequest.builder()
                                .productId(PRODUCT_ID)
                                .sku("CPU-NEW")
                                .price(UPDATED_PRICE)
                                .stock(3)
                                .attributes(LEGACY_ATTRIBUTES)
                                .build());
                    }
                })
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);

        verify(productSkuRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void changedDuplicateVariantIsRejectedAcrossUpdatePaths(boolean reconcilePath) {
        CategoryAttribute colorDefinition = definition("color", "STRING", true);
        when(categoryAttributeRepository.findByCategoryIdWithAttribute(1L))
                .thenReturn(List.of(coreCountDefinition, colorDefinition));
        product.getSkus()
                .add(ProductSku.builder()
                        .id(115L)
                        .product(product)
                        .sku("CPU-RED")
                        .attributes(Map.of("color", "Red"))
                        .active(true)
                        .build());

        assertThatThrownBy(() -> updateAttributes(reconcilePath, Map.of("color", "Red")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_SKU_ATTRIBUTES_DUPLICATED);

        assertThat(sku.getAttributes()).containsExactlyEntriesOf(LEGACY_ATTRIBUTES);
        verify(productSkuRepository, never()).save(any());
    }

    @Test
    void categoryRemainsImmutableEvenForMetadataUpdate() {
        assertThatThrownBy(() -> productService.update(
                        PRODUCT_ID,
                        ProductUpdateRequest.builder().categoryId(2L).build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_CATEGORY_CANNOT_BE_CHANGED);

        verify(productRepository, never()).save(any());
    }

    private void updateAttributes(boolean reconcilePath, Map<String, Object> attributes) {
        if (reconcilePath) {
            reconcile(existingItem(attributes));
        } else {
            productSkuService.updateSku(
                    SKU_ID,
                    ProductSkuUpdateRequest.builder()
                            .price(UPDATED_PRICE)
                            .attributes(attributes)
                            .build());
        }
    }

    private ProductSkuItemRequest existingItem(Map<String, Object> attributes) {
        return ProductSkuItemRequest.builder()
                .id(SKU_ID)
                .sku(sku.getSku())
                .price(UPDATED_PRICE)
                .stock(3)
                .attributes(attributes)
                .build();
    }

    private void reconcile(ProductSkuItemRequest item) {
        productSkuService.reconcileSkus(
                PRODUCT_ID,
                ProductSkuReconcileRequest.builder().skus(List.of(item)).build());
    }

    private List<ProductImageRequest> firstImage() {
        return List.of(ProductImageRequest.builder()
                .url("products/114/first-image.jpg")
                .sortOrder(0)
                .primary(true)
                .build());
    }

    private CategoryAttribute definition(String code, String dataType, boolean variant) {
        Attribute attribute = new Attribute();
        attribute.setCode(code);
        attribute.setDataType(dataType);
        CategoryAttribute definition = new CategoryAttribute();
        definition.setAttribute(attribute);
        definition.setIsVariantDefining(variant);
        definition.setIsRequired(false);
        definition.setIsMultiValue(false);
        return definition;
    }
}
