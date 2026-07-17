package spring.abtechzone.modules.product.service;

import java.util.*;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.request.SkuPreviewRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.dto.response.SkuPreviewResponse;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductSkuMapper;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.repository.specification.ProductSkuSpecifications;
import spring.abtechzone.modules.product.validator.ProductAttributeValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuService {

    ProductSkuRepository productSkuRepository;
    ProductRepository productRepository;
    ProductSkuMapper productSkuMapper;
    ProductAttributeValidator productAttributeValidator;
    CategoryAttributeRepository categoryAttributeRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Page<ProductSkuResponse> getSkus(ProductSkuSearchRequest request) {
        Specification<ProductSku> spec = Specification.where(ProductSkuSpecifications.hasKeyword(request.getSearch()))
                .and(ProductSkuSpecifications.hasProductId(request.getProductId()))
                .and(ProductSkuSpecifications.hasMinPrice(request.getMinPrice()))
                .and(ProductSkuSpecifications.hasMaxPrice(request.getMaxPrice()));

        return productSkuRepository.findAll(spec, request.toPageable()).map(productSkuMapper::toProductSkuResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductSkuResponse getSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));
        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse createSku(ProductSkuCreateRequest request) {
        validateSkuForCreate(request.getSku());

        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductSku sku = productSkuMapper.toProductSku(request);
        sku.setProduct(product);
        productAttributeValidator.validateSkuAttributes(product, sku.getAttributes());

        // Validate SKU duplicate variant combination
        productAttributeValidator.validateSkuNotDuplicate(product, product.getSkus(), sku.getAttributes());

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve
                    && "product_sku_sku_active_uq".equals(cve.getConstraintName())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
            throw ex;
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public ProductSkuResponse updateSku(Long skuId, ProductSkuUpdateRequest request) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        validateSkuForUpdate(skuId, request.getSku());

        Map<String, Object> updatedAttributes =
                request.getAttributes() == null ? sku.getAttributes() : request.getAttributes();
        productAttributeValidator.validateSkuAttributes(sku.getProduct(), updatedAttributes);

        // Validate SKU duplicate variant combination for updated attributes
        if (request.getAttributes() != null) {
            List<ProductSku> otherSkus = sku.getProduct().getSkus().stream()
                    .filter(s -> !s.getId().equals(skuId))
                    .toList();
            productAttributeValidator.validateSkuNotDuplicate(sku.getProduct(), otherSkus, updatedAttributes);
        }

        productSkuMapper.updateProductSku(sku, request);

        try {
            sku = productSkuRepository.save(sku);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException cve
                    && "product_sku_sku_active_uq".equals(cve.getConstraintName())) {
                throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
            }
            throw ex;
        }

        return productSkuMapper.toProductSkuResponse(sku);
    }

    @Transactional
    public void deleteSku(Long skuId) {
        ProductSku sku =
                productSkuRepository.findById(skuId).orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        Product product = sku.getProduct();
        if (product.isPublished()) {
            long remainingActive = product.getSkus().stream()
                    .filter(s -> !s.getId().equals(skuId) && Boolean.TRUE.equals(s.getIsActive()))
                    .count();
            if (remainingActive == 0) {
                throw new AppException(ErrorCode.PRODUCT_MUST_HAVE_ACTIVE_SKU);
            }
        }

        sku.softDelete();
        productSkuRepository.save(sku);
    }

    private void validateSkuForCreate(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
        }

        if (productSkuRepository.existsBySku(sku)) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
    }

    private void validateSkuForUpdate(Long skuId, String sku) {
        if (sku == null) {
            return;
        }

        if (sku.isBlank()) {
            throw new AppException(ErrorCode.PRODUCT_SKU_INVALID);
        }

        if (productSkuRepository.existsBySkuAndIdNot(sku, skuId)) {
            throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public List<SkuPreviewResponse> previewSkus(Long productId, SkuPreviewRequest request) {
        Product product =
                productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Long categoryId = product.getCategory().getId();
        List<CategoryAttribute> variantDefs =
                categoryAttributeRepository.findByCategoryIdWithAttribute(categoryId).stream()
                        .filter(ca -> Boolean.TRUE.equals(ca.getIsVariantDefining()))
                        .toList();

        if (variantDefs.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Object>> inputAttrs =
                request.getAttributes() == null ? Collections.emptyMap() : request.getAttributes();

        for (CategoryAttribute def : variantDefs) {
            String code = def.getAttribute().getCode();
            List<Object> values = inputAttrs.get(code);
            if (values == null || values.isEmpty()) {
                throw new AppException(ErrorCode.PRODUCT_SKU_VARIANT_ATTRIBUTES_MISSING);
            }

            if ("ENUM".equalsIgnoreCase(def.getAttribute().getDataType())) {
                Set<Object> allowedValues = allowedEnumValues(def.getAttribute());
                for (Object val : values) {
                    if (!allowedValues.contains(val)) {
                        throw new AppException(ErrorCode.ATTRIBUTE_VALUE_INVALID);
                    }
                }
            } else {
                throw new AppException(ErrorCode.VARIANT_ATTRIBUTE_MUST_BE_ENUM);
            }
        }

        List<Map<String, Object>> combinations = generateCartesianProduct(inputAttrs);
        return combinations.stream()
                .map(comb -> SkuPreviewResponse.builder().attributes(comb).build())
                .toList();
    }

    @Transactional
    public List<ProductSkuResponse> createSkusBulk(Long productId, List<ProductSkuCreateRequest> requests) {
        Product product =
                productRepository.findById(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductSku> currentSkus = new ArrayList<>(product.getSkus());
        List<ProductSkuResponse> savedResponses = new ArrayList<>();

        for (ProductSkuCreateRequest skuRequest : requests) {
            validateSkuForCreate(skuRequest.getSku());

            ProductSku sku = productSkuMapper.toProductSku(skuRequest);
            sku.setProduct(product);

            productAttributeValidator.validateSkuAttributes(product, sku.getAttributes());
            productAttributeValidator.validateSkuNotDuplicate(product, currentSkus, sku.getAttributes());

            try {
                sku = productSkuRepository.save(sku);
            } catch (DataIntegrityViolationException ex) {
                if (ex.getCause() instanceof ConstraintViolationException cve
                        && "product_sku_sku_active_uq".equals(cve.getConstraintName())) {
                    throw new AppException(ErrorCode.PRODUCT_SKU_EXISTS);
                }
                throw ex;
            }

            currentSkus.add(sku);
            savedResponses.add(productSkuMapper.toProductSkuResponse(sku));
        }
        return savedResponses;
    }

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
