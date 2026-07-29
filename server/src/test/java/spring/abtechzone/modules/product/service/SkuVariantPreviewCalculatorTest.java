package spring.abtechzone.modules.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.product.dto.response.SkuPreviewResponse;

class SkuVariantPreviewCalculatorTest {

    private SkuVariantPreviewCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SkuVariantPreviewCalculator();
    }

    @Test
    @DisplayName("Empty variant definitions should return empty list")
    void calculatePreview_EmptyDefs_ReturnsEmptyList() {
        List<SkuPreviewResponse> result = calculator.calculatePreview(Collections.emptyList(), Map.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Valid ENUM attribute options should produce Cartesian product combinations")
    void calculatePreview_ValidEnumOptions_GeneratesCartesianProduct() {
        Attribute colorAttr = new Attribute();
        colorAttr.setCode("color");
        colorAttr.setDataType("ENUM");
        colorAttr.setEnumValues(List.of(Map.of("value", "Red"), Map.of("value", "Blue")));

        Attribute sizeAttr = new Attribute();
        sizeAttr.setCode("size");
        sizeAttr.setDataType("ENUM");
        sizeAttr.setEnumValues(List.of(Map.of("value", "S"), Map.of("value", "M")));

        CategoryAttribute caColor = new CategoryAttribute();
        caColor.setAttribute(colorAttr);
        caColor.setIsVariantDefining(true);

        CategoryAttribute caSize = new CategoryAttribute();
        caSize.setAttribute(sizeAttr);
        caSize.setIsVariantDefining(true);

        Map<String, List<Object>> input = Map.of(
                "color", List.of("Red", "Blue"),
                "size", List.of("S", "M"));

        List<SkuPreviewResponse> responses = calculator.calculatePreview(List.of(caColor, caSize), input);

        assertThat(responses).hasSize(4);
        assertThat(responses)
                .extracting("attributes")
                .containsExactlyInAnyOrder(
                        Map.of("color", "Red", "size", "S"),
                        Map.of("color", "Red", "size", "M"),
                        Map.of("color", "Blue", "size", "S"),
                        Map.of("color", "Blue", "size", "M"));
    }

    @Test
    @DisplayName("Missing variant input should throw PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING")
    void calculatePreview_MissingInput_ThrowsException() {
        Attribute colorAttr = new Attribute();
        colorAttr.setCode("color");
        colorAttr.setDataType("ENUM");
        colorAttr.setEnumValues(List.of("Red"));

        CategoryAttribute caColor = new CategoryAttribute();
        caColor.setAttribute(colorAttr);
        caColor.setIsVariantDefining(true);

        assertThatThrownBy(() -> calculator.calculatePreview(List.of(caColor), Map.of()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING);
    }

    @Test
    @DisplayName("Invalid ENUM value not in allowed values should throw ATTRIBUTE_VALUE_INVALID")
    void calculatePreview_InvalidEnumValue_ThrowsException() {
        Attribute colorAttr = new Attribute();
        colorAttr.setCode("color");
        colorAttr.setDataType("ENUM");
        colorAttr.setEnumValues(List.of(Map.of("value", "Red")));

        CategoryAttribute caColor = new CategoryAttribute();
        caColor.setAttribute(colorAttr);
        caColor.setIsVariantDefining(true);

        Map<String, List<Object>> input = Map.of("color", List.of("Yellow"));

        assertThatThrownBy(() -> calculator.calculatePreview(List.of(caColor), input))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ATTRIBUTE_VALUE_INVALID);
    }

    @Test
    @DisplayName("Non-ENUM data type variant defining attribute should throw VARIANT_ATTRIBUTE_MUST_BE_ENUM")
    void calculatePreview_NonEnumDataType_ThrowsException() {
        Attribute numberAttr = new Attribute();
        numberAttr.setCode("weight");
        numberAttr.setDataType("NUMBER");

        CategoryAttribute caNumber = new CategoryAttribute();
        caNumber.setAttribute(numberAttr);
        caNumber.setIsVariantDefining(true);

        Map<String, List<Object>> input = Map.of("weight", List.of(100));

        assertThatThrownBy(() -> calculator.calculatePreview(List.of(caNumber), input))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VARIANT_ATTRIBUTE_MUST_BE_ENUM);
    }
}
