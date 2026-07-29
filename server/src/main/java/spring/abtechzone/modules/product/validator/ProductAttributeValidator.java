package spring.abtechzone.modules.product.validator;

import java.util.*;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.util.AttributeUtils;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductAttributeValidator {

    CategoryAttributeRepository categoryAttributeRepository;

    // ==================== PRODUCT-LEVEL (non-variant-defining) ====================

    public void validateProductAttributes(Product product) {
        requireCategory(product);
        validateAttributesMap(product.getCategory().getId(), product.getAttributes());
    }

    public void validateAttributesMap(Long categoryId, Map<String, Object> attributes) {
        Map<String, Object> attrs = attributes == null ? Collections.emptyMap() : attributes;
        Map<String, CategoryAttribute> nonVariantDefs = loadDefs(categoryId, false);

        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String code = entry.getKey();
            if (code == null || code.isBlank()) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }

            CategoryAttribute def = nonVariantDefs.get(code);
            if (def == null) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }

            validateAttributeValue(def, entry.getValue());
        }

        for (CategoryAttribute def : nonVariantDefs.values()) {
            boolean required = Boolean.TRUE.equals(def.getIsRequired());
            if (required && !attrs.containsKey(def.getAttribute().getCode())) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_REQUIRED);
            }
        }
    }

    // ==================== SKU-LEVEL (variant-defining + optional override) ====================

    public void validateSkuAttributes(Product product, Map<String, Object> skuAttributes) {
        requireCategory(product);
        Long categoryId = product.getCategory().getId();

        Map<String, CategoryAttribute> variantDefs = loadDefs(categoryId, true);
        Map<String, CategoryAttribute> nonVariantDefs = loadDefs(categoryId, false);
        Map<String, Object> attrs = skuAttributes == null ? Collections.emptyMap() : skuAttributes;

        // 1/ validate all variant-defining attributes
        for (CategoryAttribute def : variantDefs.values()) {
            String code = def.getAttribute().getCode();
            Object value = attrs.get(code);

            if (value == null) {
                throw new AppException(ErrorCode.PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING);
            }
            if (value instanceof Collection) {
                // validate variant-defining unique
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            validateScalarByType(def.getAttribute(), value);
        }

        // 2) Other Attributes: Approve only non-variant
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            if (variantDefs.containsKey(entry.getKey())) {
                continue;
            }

            CategoryAttribute def = nonVariantDefs.get(entry.getKey());
            if (def == null) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            validateAttributeValue(def, entry.getValue());
        }
    }

    public void validateProductSkus(Product product) {
        List<ProductSku> skus = product.getSkus();
        if (skus == null || skus.isEmpty()) {
            return;
        }

        for (ProductSku sku : skus) {
            validateSkuAttributes(product, sku.getAttributes());
        }

        validateNoDuplicateVariantCombination(product, skus);
    }

    public void validateSkuNotDuplicate(
            Product product, List<ProductSku> existingSkus, Map<String, Object> candidateAttributes) {
        requireCategory(product);
        Map<String, CategoryAttribute> variantDefs =
                loadDefs(product.getCategory().getId(), true);
        if (variantDefs.isEmpty()) {
            return;
        }

        Map<String, Object> candidateVariant = extractVariantValues(variantDefs, candidateAttributes);
        for (ProductSku existing : existingSkus) {
            Map<String, Object> existingVariant = extractVariantValues(variantDefs, existing.getAttributes());
            if (existingVariant.equals(candidateVariant)) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ATTRIBUTES_DUPLICATED);
            }
        }
    }

    // ==================== helpers ====================

    private void requireCategory(Product product) {
        if (product.getCategory() == null || product.getCategory().getId() == null) {
            throw new AppException(ErrorCode.CATEGORY_REQUIRED);
        }
    }

    private Map<String, CategoryAttribute> loadDefs(Long categoryId, boolean variantDefining) {
        List<CategoryAttribute> all = categoryAttributeRepository.findByCategoryIdWithAttribute(categoryId);
        Map<String, CategoryAttribute> result = new HashMap<>();
        for (CategoryAttribute ca : all) {
            if (Boolean.TRUE.equals(ca.getIsVariantDefining()) == variantDefining) {
                result.put(ca.getAttribute().getCode(), ca);
            }
        }
        return result;
    }

    private void validateNoDuplicateVariantCombination(Product product, List<ProductSku> skus) {
        Map<String, CategoryAttribute> variantDefs =
                loadDefs(product.getCategory().getId(), true);
        if (variantDefs.isEmpty()) {
            return;
        }

        Set<Map<String, Object>> seen = new HashSet<>();
        for (ProductSku sku : skus) {
            Map<String, Object> variantValues = extractVariantValues(variantDefs, sku.getAttributes());
            if (!seen.add(variantValues)) {
                throw new AppException(ErrorCode.PRODUCT_SKU_ATTRIBUTES_DUPLICATED);
            }
        }
    }

    private Map<String, Object> extractVariantValues(
            Map<String, CategoryAttribute> variantDefs, Map<String, Object> attrs) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> source = attrs == null ? Collections.emptyMap() : attrs;
        for (String code : variantDefs.keySet()) {
            result.put(code, source.get(code));
        }
        return result;
    }

    /**
     * Validate 1 value: isMultiValue .
     */
    private void validateAttributeValue(CategoryAttribute def, Object value) {
        boolean multi = Boolean.TRUE.equals(def.getIsMultiValue());

        if (multi) {
            if (!(value instanceof Collection<?> col) || col.isEmpty()) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            Set<Object> seen = new HashSet<>();
            for (Object item : col) {
                if (item == null || !seen.add(item)) {
                    throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
                }
                validateScalarByType(def.getAttribute(), item);
            }
        } else {
            if (value instanceof Collection) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            validateScalarByType(def.getAttribute(), value);
        }
    }

    /**
     * Validate 1 unique scalar value of dataType.
     */
    private void validateScalarByType(Attribute def, Object value) {
        if (value == null) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        String dataType = def.getDataType();
        if ("STRING".equalsIgnoreCase(dataType)) {
            validateStringScalar(def, value);
        } else if ("NUMBER".equalsIgnoreCase(dataType)) {
            validateNumberScalar(def, value);
        } else if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            validateBooleanScalar(value);
        } else if ("ENUM".equalsIgnoreCase(dataType)) {
            validateEnumScalar(def, value);
        } else {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
    }

    private void validateStringScalar(Attribute def, Object value) {
        if (!(value instanceof String str) || str.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
        if (hasEnumValues(def) && !AttributeUtils.extractAllowedEnumValues(def).contains(value)) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
    }

    private void validateBooleanScalar(Object value) {
        if (!(value instanceof Boolean)) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
    }

    private void validateEnumScalar(Attribute def, Object value) {
        if (hasEnumValues(def)) {
            if (!AttributeUtils.extractAllowedEnumValues(def).contains(value)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if (!(value instanceof String str) || str.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
    }

    private void validateNumberScalar(Attribute def, Object value) {
        if (!(value instanceof Number numberValue)) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
        if (hasEnumValues(def) && !isNumberValueAllowed(def, numberValue)) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
    }

    private boolean isNumberValueAllowed(Attribute def, Number value) {
        double valDouble = value.doubleValue();
        for (Object allowed : AttributeUtils.extractAllowedEnumValues(def)) {
            if (matchesNumberValue(allowed, valDouble, value)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesNumberValue(Object allowed, double valDouble, Object value) {
        if (allowed instanceof Number allowedNum) {
            return allowedNum.doubleValue() == valDouble;
        }
        if (allowed instanceof String allowedStr) {
            try {
                if (Double.parseDouble(allowedStr) == valDouble) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Expected when allowed enum value string is non-numeric
            }
        }
        return allowed.toString().equals(value.toString());
    }

    private boolean hasEnumValues(Attribute def) {
        return def.getEnumValues() != null && !def.getEnumValues().isEmpty();
    }
}
