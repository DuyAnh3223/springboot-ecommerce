package spring.abtechzone.modules.product.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.BrandRepository;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.product.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.mapper.ProductMapper;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    ProductMapper productMapper;

    @Mock
    ProductSkuService productSkuService;

    @Mock
    ProductAttributeValidator productAttributeValidator;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    BrandRepository brandRepository;

    @InjectMocks
    ProductService productService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Laptops");
    }

    @Test
    @DisplayName("create product with unknown brandId should throw BRAND_NOT_FOUND")
    void create_UnknownBrand_ThrowsBrandNotFound() {
        ProductCreateRequest request = ProductCreateRequest.builder()
                .name("Laptop HighEnd")
                .categoryId(1L)
                .brandId(999L)
                .build();

        Product product = Product.builder().name("Laptop HighEnd").build();

        when(productMapper.toProduct(request)).thenReturn(product);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
    }

    @Test
    @DisplayName("update product with unknown brandId should throw BRAND_NOT_FOUND")
    void update_UnknownBrand_ThrowsBrandNotFound() {
        Product existingProduct = Product.builder()
                .id(10L)
                .name("Laptop HighEnd")
                .category(sampleCategory)
                .build();

        ProductUpdateRequest request =
                ProductUpdateRequest.builder().brandId(999L).build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(existingProduct));
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(10L, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BRAND_NOT_FOUND);
    }
}
