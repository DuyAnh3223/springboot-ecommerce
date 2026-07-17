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

/**
 * Quy tac chung:
 * - Thuoc tinh KHONG variant-defining (isVariantDefining = false) chi duoc khai bao o Product.attributes.
 * Neu isRequired = true thi bat buoc phai co trong Product.attributes.
 * Neu isMultiValue = true thi gia tri phai la Collection (nhieu gia tri cung luc, vd Part#, CPU Socket).
 * - Thuoc tinh CO variant-defining (isVariantDefining = true) bat buoc phai co o MOI ProductSku.attributes,
 * moi SKU dung DUNG 1 gia tri scalar (khong duoc la Collection, khong duoc thieu).
 * - ProductSku.attributes CO THE chua them key thuoc nhom non-variant-defining de OVERRIDE gia tri
 * rieng cho SKU do (vd 1 mau co backplate khac support them 1 CPU socket) - day la optional,
 * khong bat buoc phai co du toan bo non-variant attribute o SKU.
 * - Khong cho 2 SKU cua cung 1 Product co CUNG mot to hop gia tri o cac thuoc tinh variant-defining.
 */
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
                // Key khong ton tai trong nhom non-variant cua category nay -
                // co the do go sai code, hoac day la key variant-defining (khong duoc phep o Product)
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

        // 1) Cac thuoc tinh variant-defining: bat buoc co du, moi cai dung 1 gia tri scalar hop le
        for (CategoryAttribute def : variantDefs.values()) {
            String code = def.getAttribute().getCode();
            Object value = attrs.get(code);

            if (value == null) {
                throw new AppException(ErrorCode.PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING);
            }
            if (value instanceof Collection) {
                // variant-defining luon la 1 gia tri duy nhat, khong cho phep mang
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            validateScalarByType(def.getAttribute(), value);
        }

        // 2) Cac key con lai (neu co): chi chap nhan neu la override hop le cua nhom non-variant
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

    /**
     * Dung khi them 1 SKU moi vao product da ton tai (ProductSkuService.createSku),
     * luc nay khong co san toan bo list SKU trong 1 request nen phai truyen existingSkus
     * (fetch tu DB) de so sanh.
     */
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

    public void validateExistingSkusAgainstUpdatedAttributes(Product product, Map<String, Object> updatedAttributes) {
        requireCategory(product);
        // Gia tri hop le cua SKU khong con phu thuoc vao Product.attributes (da tach rieng theo
        // isVariantDefining o CategoryAttribute), nen chi can validate lai map attributes moi cua
        // Product theo dinh nghia category - khong can duyet lai tung SKU o day.
        validateAttributesMap(product.getCategory().getId(), updatedAttributes);
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
     * Validate 1 gia tri (scalar hoac list) theo cau hinh isMultiValue cua CategoryAttribute.
     */
    private void validateAttributeValue(CategoryAttribute def, Object value) {
        boolean multi = Boolean.TRUE.equals(def.getIsMultiValue());

        if (multi) {
            if (!(value instanceof Collection)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
            Collection<?> col = (Collection<?>) value;
            if (col.isEmpty()) {
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
     * Validate 1 gia tri scalar duy nhat theo dataType cua Attribute.
     */
    private void validateScalarByType(Attribute def, Object value) {
        if (value == null) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }

        String dataType = def.getDataType();

        if ("STRING".equalsIgnoreCase(dataType)) {
            if (!(value instanceof String) || ((String) value).isBlank()) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if ("NUMBER".equalsIgnoreCase(dataType)) {
            if (!(value instanceof Number)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            if (!(value instanceof Boolean)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else if ("ENUM".equalsIgnoreCase(dataType)) {
            if (!allowedEnumValues(def).contains(value)) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
            }
        } else {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTES_INVALID);
        }
    }

    /**
     * Trich tap gia tri hop le tu Attribute.enumValues.
     */
    private Set<Object> allowedEnumValues(Attribute def) {
        List<Object> options = def.getEnumValues();
        if (options == null || options.isEmpty()) {
            throw new AppException(ErrorCode.ATTRIBUTE_ENUM_VALUES_MISSING);
        }

        Set<Object> allowed = new HashSet<>();
        for (Object opt : options) {
            if (opt instanceof Map) {
                Object v = ((Map<?, ?>) opt).get("value");
                if (v != null) {
                    allowed.add(v);
                }
            } else {
                allowed.add(opt);
            }
        }
        return allowed;
    }
}
