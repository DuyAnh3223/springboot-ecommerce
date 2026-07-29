package spring.abtechzone.modules.product.service;

import java.util.*;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.product.dto.response.SkuPreviewResponse;
import spring.abtechzone.modules.product.util.AttributeUtils;

public class SkuVariantPreviewCalculator {

    List<SkuPreviewResponse> calculatePreview(
            List<CategoryAttribute> variantDefs, Map<String, List<Object>> inputAttrs) {
        if (variantDefs == null || variantDefs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Object>> safeInputAttrs = inputAttrs == null ? Collections.emptyMap() : inputAttrs;
        validateVariantAttributes(variantDefs, safeInputAttrs);

        List<Map<String, Object>> combinations = generateCartesianProduct(safeInputAttrs);
        return combinations.stream()
                .map(comb -> SkuPreviewResponse.builder().attributes(comb).build())
                .toList();
    }

    private void validateVariantAttributes(
            List<CategoryAttribute> variantDefs, Map<String, List<Object>> safeInputAttrs) {
        for (CategoryAttribute def : variantDefs) {
            validateVariantAttribute(def, safeInputAttrs);
        }
    }

    private void validateVariantAttribute(CategoryAttribute def, Map<String, List<Object>> safeInputAttrs) {
        Attribute attribute = def.getAttribute();
        if (!"ENUM".equalsIgnoreCase(attribute.getDataType())) {
            throw new AppException(ErrorCode.VARIANT_ATTRIBUTE_MUST_BE_ENUM);
        }

        List<Object> values = safeInputAttrs.get(attribute.getCode());
        if (values == null || values.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING);
        }

        validateAttributeValues(attribute, values);
    }

    private void validateAttributeValues(Attribute attribute, List<Object> values) {
        List<Object> options = attribute.getEnumValues();
        if (options != null && !options.isEmpty()) {
            Set<Object> allowedValues = AttributeUtils.extractAllowedEnumValues(attribute);
            for (Object val : values) {
                if (!allowedValues.contains(val)) {
                    throw new AppException(ErrorCode.ATTRIBUTE_VALUE_INVALID);
                }
            }
        } else {
            for (Object val : values) {
                if (!(val instanceof String str) || str.isBlank()) {
                    throw new AppException(ErrorCode.ATTRIBUTE_VALUE_INVALID);
                }
            }
        }
    }

    private List<Map<String, Object>> generateCartesianProduct(Map<String, List<Object>> input) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return result;
        }

        List<String> keys = new ArrayList<>(input.keySet());
        generateCombinations(input, keys, 0, new HashMap<>(), result);
        return result;
    }

    private void generateCombinations(
            Map<String, List<Object>> input,
            List<String> keys,
            int depth,
            Map<String, Object> current,
            List<Map<String, Object>> result) {
        if (depth == keys.size()) {
            result.add(new HashMap<>(current));
            return;
        }

        String key = keys.get(depth);
        List<Object> values = input.get(key);
        if (values == null || values.isEmpty()) {
            generateCombinations(input, keys, depth + 1, current, result);
        } else {
            for (Object val : values) {
                current.put(key, val);
                generateCombinations(input, keys, depth + 1, current, result);
                current.remove(key);
            }
        }
    }
}
