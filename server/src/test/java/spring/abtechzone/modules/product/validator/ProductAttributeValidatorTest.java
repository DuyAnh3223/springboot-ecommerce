package spring.abtechzone.modules.product.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;

@ExtendWith(MockitoExtension.class)
class ProductAttributeValidatorTest {

    @Mock
    CategoryAttributeRepository categoryAttributeRepository;

    @InjectMocks
    ProductAttributeValidator productAttributeValidator;

    Product sampleProduct;
    Category sampleCategory;

    private CategoryAttribute createVarCategoryAttribute(String code, String dataType) {
        Attribute attr = new Attribute();
        attr.setCode(code);
        attr.setDataType(dataType);

        CategoryAttribute ca = new CategoryAttribute();
        ca.setCategory(sampleCategory);
        ca.setAttribute(attr);
        ca.setIsVariantDefining(true);
        return ca;
    }

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Laptops");

        sampleProduct = Product.builder()
                .id(100L)
                .name("Test Laptop")
                .category(sampleCategory)
                .build();
    }

    @Test
    @DisplayName(
            "validateSkuNotDuplicate should throw exception when candidate attributes match existing SKU variant attributes")
    void validateSkuNotDuplicate_DuplicateVariantCombination_ShouldThrowException() {
        CategoryAttribute colorAttr = createVarCategoryAttribute("color", "STRING");
        CategoryAttribute ramAttr = createVarCategoryAttribute("ram", "STRING");

        when(categoryAttributeRepository.findByCategoryIdWithAttribute(1L)).thenReturn(List.of(colorAttr, ramAttr));

        ProductSku existingSku = ProductSku.builder()
                .id(10L)
                .sku("SKU-RED-16GB")
                .attributes(Map.of("color", "red", "ram", "16gb"))
                .build();

        Map<String, Object> candidateDuplicateAttrs = Map.of("color", "red", "ram", "16gb");

        assertThatThrownBy(() -> productAttributeValidator.validateSkuNotDuplicate(
                        sampleProduct, List.of(existingSku), candidateDuplicateAttrs))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName(
            "validateSkuNotDuplicate should pass when candidate attributes differ from existing SKU variant attributes")
    void validateSkuNotDuplicate_DifferentVariantCombination_ShouldPass() {
        CategoryAttribute colorAttr = createVarCategoryAttribute("color", "STRING");
        CategoryAttribute ramAttr = createVarCategoryAttribute("ram", "STRING");

        when(categoryAttributeRepository.findByCategoryIdWithAttribute(1L)).thenReturn(List.of(colorAttr, ramAttr));

        ProductSku existingSku = ProductSku.builder()
                .id(10L)
                .sku("SKU-RED-16GB")
                .attributes(Map.of("color", "red", "ram", "16gb"))
                .build();

        Map<String, Object> candidateUniqueAttrs = Map.of("color", "blue", "ram", "16gb");

        assertThatCode(() -> productAttributeValidator.validateSkuNotDuplicate(
                        sampleProduct, List.of(existingSku), candidateUniqueAttrs))
                .doesNotThrowAnyException();
    }
}
