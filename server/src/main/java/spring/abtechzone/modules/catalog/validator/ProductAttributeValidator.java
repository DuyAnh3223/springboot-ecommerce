package spring.abtechzone.modules.catalog.validator;

import java.util.*;

import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.entity.Attribute;
import spring.abtechzone.modules.catalog.entity.CategoryAttribute;
import spring.abtechzone.modules.catalog.entity.Product;
import spring.abtechzone.modules.catalog.entity.ProductSku;
import spring.abtechzone.modules.catalog.repository.CategoryAttributeRepository;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductAttributeValidator {

    CategoryAttributeRepository categoryAttributeRepository;

    public void validateProductAttributes(Product product) {
        validateAttributesMap(product.getCategory().getId(), product.getAttributes());
    }

    public void validateAttributesMap(Long categoryId, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        toAllowedAttributes(attributes);

        List<CategoryAttribute> categoryAttributes =
                categoryAttributeRepository.findByCategoryIdWithAttribute(categoryId);
        Map<String, Attribute> defMap = new HashMap<>();
        for (CategoryAttribute ca : categoryAttributes) {
            defMap.put(ca.getAttribute().getCode(), ca.getAttribute());
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Attribute def = defMap.get(key);
            if (def == null) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }

            validateType(def, value);
        }
    }

    private void validateType(Attribute def, Object value) {
        if (value == null) {
            return;
        }

        String dataType = def.getDataType();
        if ("STRING".equalsIgnoreCase(dataType)) {
            if (!(value instanceof String || value instanceof Collection)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if ("NUMBER".equalsIgnoreCase(dataType)) {
            if (!(value instanceof Number || value instanceof Collection)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            if (!(value instanceof Boolean || value instanceof Collection)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if ("ENUM".equalsIgnoreCase(dataType)) {
            Object enumValuesObj = def.getEnumValues();
            Collection<?> allowedValues = null;
            if (enumValuesObj instanceof Collection) {
                allowedValues = (Collection<?>) enumValuesObj;
            } else if (enumValuesObj instanceof Map) {
                allowedValues = ((Map<?, ?>) enumValuesObj).keySet();
            }

            if (allowedValues == null) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }

            if (value instanceof Collection) {
                for (Object item : (Collection<?>) value) {
                    if (!allowedValues.contains(item)) {
                        throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
                    }
                }
            } else {
                if (!allowedValues.contains(value)) {
                    throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
                }
            }
        }
    }

    public void validateSkuAttributes(Product product, Map<String, Object> skuAttributes) {
        validateProductAttributes(product);
        validateSkuAttributes(toAllowedAttributes(product.getAttributes()), skuAttributes);
    }

    public void validateProductSkus(Product product) {
        if (product.getSkus() == null) {
            return;
        }

        Map<String, Set<Object>> allowedAttributes = toAllowedAttributes(product.getAttributes());
        for (ProductSku sku : product.getSkus()) {
            validateSkuAttributes(allowedAttributes, sku.getAttributes());
        }
    }

    public void validateExistingSkusAgainstUpdatedAttributes(Product product, Map<String, Object> updatedAttributes) {
        validateAttributesMap(product.getCategory().getId(), updatedAttributes);

        if (product.getSkus() == null) {
            return;
        }

        Map<String, Set<Object>> allowedAttributes = toAllowedAttributes(updatedAttributes);
        for (ProductSku sku : product.getSkus()) {
            validateSkuAttributes(allowedAttributes, sku.getAttributes());
        }
    }

    private void validateSkuAttributes(Map<String, Set<Object>> allowedAttributes, Map<String, Object> skuAttributes) {
        if (allowedAttributes.isEmpty()) {
            if (skuAttributes != null && !skuAttributes.isEmpty()) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            return;
        }

        if (skuAttributes == null || skuAttributes.size() != allowedAttributes.size()) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        for (Map.Entry<String, Set<Object>> allowedAttribute : allowedAttributes.entrySet()) {
            Object skuValue = skuAttributes.get(allowedAttribute.getKey());
            if (skuValue == null || !allowedAttribute.getValue().contains(skuValue)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        }
    }

    private Map<String, Set<Object>> toAllowedAttributes(Map<String, Object> attributes) {
        Map<String, Set<Object>> allowedAttributes = new HashMap<>();
        if (attributes == null) {
            return allowedAttributes;
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }

            Object val = entry.getValue();
            Set<Object> set = new HashSet<>();
            if (val instanceof Collection) {
                Collection<?> col = (Collection<?>) val;
                for (Object item : col) {
                    if (item == null || (item instanceof String && ((String) item).isBlank())) {
                        throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
                    }
                    if (!set.add(item)) {
                        throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
                    }
                }
            } else if (val != null) {
                if (val instanceof String && ((String) val).isBlank()) {
                    throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
                }
                set.add(val);
            }
            allowedAttributes.put(key, set);
        }

        return allowedAttributes;
    }
}
