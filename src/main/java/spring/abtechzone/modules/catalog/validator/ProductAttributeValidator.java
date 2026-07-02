package spring.abtechzone.modules.catalog.validator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.entity.Product;
import spring.abtechzone.modules.catalog.entity.ProductAttribute;
import spring.abtechzone.modules.catalog.entity.ProductSku;

@Component
public class ProductAttributeValidator {

    public void validateProductAttributes(List<ProductAttribute> attributes) {
        if (attributes == null) {
            return;
        }

        Set<String> attributeNames = new HashSet<>();
        for (ProductAttribute attribute : attributes) {
            validateSingleAttribute(attribute, attributeNames);
        }
    }

    private void validateSingleAttribute(ProductAttribute attribute, Set<String> attributeNames) {
        if (attribute == null || isBlank(attribute.getName())) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        if (attribute.getValues() == null || attribute.getValues().isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        if (!attributeNames.add(attribute.getName())) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        validateAttributeValues(attribute.getValues());
    }

    private void validateAttributeValues(List<String> values) {
        Set<String> uniqueValues = new HashSet<>();
        for (String value : values) {
            if (isBlank(value) || !uniqueValues.add(value)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public void validateProductSkus(Product product) {
        if (product.getSkus() == null) {
            return;
        }

        Map<String, Set<String>> allowedAttributes = toAllowedAttributes(product.getAttributes());
        for (ProductSku sku : product.getSkus()) {
            validateSkuAttributes(allowedAttributes, sku.getAttributes());
        }
    }

    public void validateSkuAttributes(Product product, Map<String, String> skuAttributes) {
        validateProductAttributes(product.getAttributes());
        validateSkuAttributes(toAllowedAttributes(product.getAttributes()), skuAttributes);
    }

    public void validateExistingSkusAgainstUpdatedAttributes(
            Product product, List<ProductAttribute> updatedAttributes) {
        if (updatedAttributes == null) {
            return;
        }

        validateProductAttributes(updatedAttributes);

        if (product.getSkus() == null) {
            return;
        }

        Map<String, Set<String>> allowedAttributes = toAllowedAttributes(updatedAttributes);
        for (ProductSku sku : product.getSkus()) {
            validateSkuAttributes(allowedAttributes, sku.getAttributes());
        }
    }

    private void validateSkuAttributes(Map<String, Set<String>> allowedAttributes, Map<String, String> skuAttributes) {
        if (allowedAttributes.isEmpty()) {
            if (skuAttributes != null && !skuAttributes.isEmpty()) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            return;
        }

        if (skuAttributes == null || skuAttributes.size() != allowedAttributes.size()) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        for (Map.Entry<String, Set<String>> allowedAttribute : allowedAttributes.entrySet()) {
            String skuValue = skuAttributes.get(allowedAttribute.getKey());
            if (skuValue == null || !allowedAttribute.getValue().contains(skuValue)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        }
    }

    private Map<String, Set<String>> toAllowedAttributes(List<ProductAttribute> attributes) {
        Map<String, Set<String>> allowedAttributes = new HashMap<>();
        if (attributes == null) {
            return allowedAttributes;
        }

        for (ProductAttribute attribute : attributes) {
            allowedAttributes.put(attribute.getName(), new HashSet<>(attribute.getValues()));
        }

        return allowedAttributes;
    }
}
