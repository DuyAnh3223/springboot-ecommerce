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
                .stock(10)
                .imageUrl("products/10/old-primary.png")
                .build();
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
}
