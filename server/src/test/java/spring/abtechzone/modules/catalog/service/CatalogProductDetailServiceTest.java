package spring.abtechzone.modules.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.common.service.AwsS3FileService;
import spring.abtechzone.modules.catalog.dto.response.CatalogProductDetailResponse;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.Brand;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductImage;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.service.ProductService;

@ExtendWith(MockitoExtension.class)
class CatalogProductDetailServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CategoryAttributeRepository categoryAttributeRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductService productService;

    @Mock
    ProductImageRepository productImageRepository;

    @Mock
    AwsS3FileService awsS3FileService;

    @Mock
    InventoryService inventoryService;

    @InjectMocks
    CatalogService catalogService;

    @Test
    void getProductDetail_mapsOnlyActiveSkusAndCustomerMetadata() {
        Category category = category(true);
        Brand brand = new Brand();
        brand.setId(3L);
        brand.setName("AMD");
        brand.setSlug("amd");

        Product product = product(category);
        product.setBrand(brand);
        product.setAttributes(Map.of("socket", "AM4", "core_count", 6));
        product.setRating(4.8);
        product.setReviewCount(12);

        ProductImage primaryImage = image(501L, "products/product.webp", 0, true);
        ProductSku active = sku(129L, product, "AMD-R5-5500-BOX", 3_429_000, 9, true);
        active.setAttributes(Map.of("package", "BOX"));
        active.setImageUrl("products/sku.webp");
        active.setImages(List.of(
                image(601L, "products/sku-secondary.webp", 2, false),
                image(600L, "products/sku-primary.webp", 1, true)));
        ProductSku inactive = sku(130L, product, "AMD-R5-5500-TRAY", 3_299_000, 20, false);
        product.setSkus(List.of(active, inactive));

        CategoryAttribute specification = categoryAttribute(category, "socket", "Socket", false, 10);
        CategoryAttribute variant = categoryAttribute(category, "package", "Phiên bản", true, 20);

        ProductImageRepository.ProductPrimaryImageProjection projection =
                org.mockito.Mockito.mock(ProductImageRepository.ProductPrimaryImageProjection.class);
        when(projection.getUrl()).thenReturn(primaryImage.getUrl());
        when(productRepository.findBySlug("amd-ryzen-5-5500")).thenReturn(Optional.of(product));
        when(categoryAttributeRepository.findByCategoryIdWithAttribute(7L)).thenReturn(List.of(specification, variant));
        when(productImageRepository.findPrimaryImagesByProductIds(List.of(101L)))
                .thenReturn(List.of(projection));
        when(inventoryService.getOnHandBySkuIds(List.of(129L))).thenReturn(Map.of(129L, 9));
        when(awsS3FileService.resolveAccessUrl("products/product.webp")).thenReturn("https://cdn.example/product.webp");
        when(awsS3FileService.resolveAccessUrl("products/sku.webp")).thenReturn("https://cdn.example/sku.webp");
        when(awsS3FileService.resolveAccessUrl("products/sku-primary.webp"))
                .thenReturn("https://cdn.example/sku-primary.webp");
        when(awsS3FileService.resolveAccessUrl("products/sku-secondary.webp"))
                .thenReturn("https://cdn.example/sku-secondary.webp");

        CatalogProductDetailResponse response = catalogService.getProductDetail("amd-ryzen-5-5500");

        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getCategory().getSlug()).isEqualTo("cpu");
        assertThat(response.getBrand().getSlug()).isEqualTo("amd");
        assertThat(response.getPrimaryImageUrl()).isEqualTo("https://cdn.example/product.webp");
        assertThat(response.getPriceMin()).isEqualByComparingTo("3429000");
        assertThat(response.getPriceMax()).isEqualByComparingTo("3429000");
        assertThat(response.getTotalStock()).isEqualTo(9);
        assertThat(response.getSkus()).hasSize(1);
        assertThat(response.getSkus().getFirst().getId()).isEqualTo(129L);
        assertThat(response.getSkus().getFirst().getPrimaryImageUrl()).isEqualTo("https://cdn.example/sku.webp");
        assertThat(response.getSkus().getFirst().getImages())
                .extracting(CatalogProductDetailResponse.Image::getUrl)
                .containsExactly("https://cdn.example/sku-primary.webp", "https://cdn.example/sku-secondary.webp");
        assertThat(response.getSpecificationDefinitions())
                .extracting(CatalogProductDetailResponse.AttributeDefinition::getCode)
                .containsExactly("socket", "package");
        assertThat(response.getVariantDefinitions())
                .extracting(CatalogProductDetailResponse.AttributeDefinition::getCode)
                .containsExactly("package");
    }

    @Test
    void getProductDetail_withNoActiveSku_returnsUnavailableProduct() {
        Product product = product(category(true));
        product.setSkus(List.of(sku(130L, product, "INACTIVE", 100, 5, false)));
        when(productRepository.findBySlug(product.getSlug())).thenReturn(Optional.of(product));
        when(categoryAttributeRepository.findByCategoryIdWithAttribute(7L)).thenReturn(List.of());
        when(productImageRepository.findPrimaryImagesByProductIds(List.of(101L)))
                .thenReturn(List.of());

        CatalogProductDetailResponse response = catalogService.getProductDetail(product.getSlug());

        assertThat(response.getSkus()).isEmpty();
        assertThat(response.getPriceMin()).isNull();
        assertThat(response.getPriceMax()).isNull();
        assertThat(response.getTotalStock()).isZero();
    }

    @Test
    void getProductDetail_forNonPublicStates_returnsSameProductNotFoundError() {
        Product draft = product(category(true));
        draft.setDraft(true);
        assertProductNotFound(draft);

        Product unpublished = product(category(true));
        unpublished.setPublished(false);
        assertProductNotFound(unpublished);

        Product inactiveCategory = product(category(false));
        assertProductNotFound(inactiveCategory);

        Product deleted = product(category(true));
        deleted.setDeletedAt(OffsetDateTime.now());
        assertProductNotFound(deleted);
    }

    private void assertProductNotFound(Product product) {
        when(productRepository.findBySlug(product.getSlug())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> catalogService.getProductDetail(product.getSlug()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    private Product product(Category category) {
        return Product.builder()
                .id(101L)
                .name("AMD Ryzen 5 5500")
                .slug("amd-ryzen-5-5500")
                .description("Bộ xử lý AMD Ryzen 5 5500")
                .published(true)
                .draft(false)
                .category(category)
                .attributes(Map.of())
                .reviewCount(0)
                .skus(List.of())
                .build();
    }

    private Category category(boolean active) {
        Category category = new Category();
        category.setId(7L);
        category.setName("CPU");
        category.setSlug("cpu");
        category.setIsActive(active);
        return category;
    }

    private ProductSku sku(Long id, Product product, String code, long price, int stock, boolean active) {
        return ProductSku.builder()
                .id(id)
                .product(product)
                .sku(code)
                .price(BigDecimal.valueOf(price))
                .currency("VND")
                .active(active)
                .attributes(Map.of())
                .images(List.of())
                .build();
    }

    private ProductImage image(Long id, String url, int sortOrder, boolean primary) {
        return ProductImage.builder()
                .id(id)
                .url(url)
                .sortOrder(sortOrder)
                .primary(primary)
                .build();
    }

    private CategoryAttribute categoryAttribute(
            Category category, String code, String name, boolean variantDefining, int sortOrder) {
        Attribute attribute = new Attribute();
        attribute.setCode(code);
        attribute.setName(name);
        attribute.setDataType("STRING");

        CategoryAttribute categoryAttribute = new CategoryAttribute();
        categoryAttribute.setCategory(category);
        categoryAttribute.setAttribute(attribute);
        categoryAttribute.setIsVariantDefining(variantDefining);
        categoryAttribute.setSortOrder(sortOrder);
        return categoryAttribute;
    }
}
