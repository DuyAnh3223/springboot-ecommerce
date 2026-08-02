package spring.abtechzone.modules.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import spring.abtechzone.modules.catalog.dto.request.CatalogSearchRequest;
import spring.abtechzone.modules.catalog.dto.response.CategoryFacetResponse;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.category.repository.CategoryAttributeRepository;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.product.dto.response.ProductResponse;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.repository.ProductImageRepository;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.specification.CatalogSpecifications;
import spring.abtechzone.modules.product.repository.specification.ProductSpecifications;
import spring.abtechzone.modules.product.service.ProductService;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogService {

    CategoryRepository categoryRepository;
    CategoryAttributeRepository categoryAttributeRepository;
    ProductRepository productRepository;
    ProductService productService;
    ProductImageRepository productImageRepository;
    spring.abtechzone.common.service.AwsS3FileService awsS3FileService;

    @Transactional(readOnly = true)
    public CategoryFacetResponse getCategoryFacets(String categorySlug) {
        Category category = categoryRepository
                .findBySlugAndIsActiveTrue(categorySlug)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Long categoryId = category.getId();
        List<CategoryAttribute> categoryAttributes =
                categoryAttributeRepository.findByCategoryIdWithAttribute(categoryId);

        List<CategoryFacetResponse.BrandFacetResponse> brands = fetchBrandFacets(categoryId);
        BigDecimal[] priceBounds = fetchCategoryPriceBounds(categoryId);
        List<CategoryFacetResponse.AttributeFacetResponse> attrFacets =
                buildAttributeFacets(categoryId, categoryAttributes);

        return CategoryFacetResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .categorySlug(category.getSlug())
                .brands(brands)
                .priceMin(priceBounds[0])
                .priceMax(priceBounds[1])
                .attributes(attrFacets)
                .build();
    }

    private List<CategoryFacetResponse.BrandFacetResponse> fetchBrandFacets(Long categoryId) {
        List<Object[]> brandRows = productRepository.findBrandsByCategoryId(categoryId);
        return brandRows.stream()
                .map(row -> CategoryFacetResponse.BrandFacetResponse.builder()
                        .id(((Number) row[0]).longValue())
                        .name((String) row[1])
                        .slug((String) row[2])
                        .build())
                .toList();
    }

    private BigDecimal[] fetchCategoryPriceBounds(Long categoryId) {
        Object rawPriceBounds = productRepository.findPriceBoundsByCategoryId(categoryId);
        Object[] priceBounds = rawPriceBounds instanceof Object[] boundsArr ? boundsArr : null;

        BigDecimal min =
                (priceBounds != null && priceBounds[0] != null) ? (BigDecimal) priceBounds[0] : BigDecimal.ZERO;
        BigDecimal max =
                (priceBounds != null && priceBounds[1] != null) ? (BigDecimal) priceBounds[1] : BigDecimal.ZERO;
        return new BigDecimal[] {min, max};
    }

    private List<CategoryFacetResponse.AttributeFacetResponse> buildAttributeFacets(
            Long categoryId, List<CategoryAttribute> categoryAttributes) {
        return categoryAttributes.stream()
                .map(ca -> buildSingleAttributeFacet(categoryId, ca))
                .toList();
    }

    private CategoryFacetResponse.AttributeFacetResponse buildSingleAttributeFacet(
            Long categoryId, CategoryAttribute ca) {
        BigDecimal[] numberBounds = fetchAttributeNumberBounds(categoryId, ca);

        return CategoryFacetResponse.AttributeFacetResponse.builder()
                .attributeId(ca.getAttribute().getId())
                .code(ca.getAttribute().getCode())
                .name(ca.getAttribute().getName())
                .dataType(ca.getAttribute().getDataType())
                .unit(ca.getAttribute().getUnit())
                .enumValues(ca.getAttribute().getEnumValues())
                .isFilterable(ca.getIsFilterable())
                .isSortable(ca.getIsSortable())
                .isVariantDefining(ca.getIsVariantDefining())
                .isMultiValue(ca.getIsMultiValue())
                .sortOrder(ca.getSortOrder())
                .minBound(numberBounds[0])
                .maxBound(numberBounds[1])
                .build();
    }

    private BigDecimal[] fetchAttributeNumberBounds(Long categoryId, CategoryAttribute ca) {
        if (!"NUMBER".equalsIgnoreCase(ca.getAttribute().getDataType()) || !Boolean.TRUE.equals(ca.getIsFilterable())) {
            return new BigDecimal[] {null, null};
        }

        String code = ca.getAttribute().getCode();
        try {
            Object rawBounds = productRepository.findAttributeNumberBounds(code, categoryId);
            Object[] bounds = rawBounds instanceof Object[] boundsArr ? boundsArr : null;
            if (bounds != null) {
                BigDecimal min = bounds[0] != null ? new BigDecimal(bounds[0].toString()) : null;
                BigDecimal max = bounds[1] != null ? new BigDecimal(bounds[1].toString()) : null;
                return new BigDecimal[] {min, max};
            }
        } catch (Exception e) {
            log.warn("Failed to compute number bounds for attribute code={}", code, e);
        }

        return new BigDecimal[] {null, null};
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getCatalogProducts(String categorySlug, CatalogSearchRequest request) {
        Category category = categoryRepository
                .findBySlugAndIsActiveTrue(categorySlug)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        List<CategoryAttribute> categoryAttributes =
                categoryAttributeRepository.findByCategoryIdWithAttribute(category.getId());

        Specification<Product> spec = Specification.where(ProductSpecifications.isPublished())
                .and(ProductSpecifications.hasActiveCategory(category.getId()));

        if (request.getSearch() != null && !request.getSearch().isBlank()) {
            spec = spec.and(ProductSpecifications.hasKeyword(request.getSearch()));
        }
        if (request.getBrandId() != null) {
            spec = spec.and(CatalogSpecifications.hasBrand(request.getBrandId()));
        }
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            spec = spec.and(CatalogSpecifications.priceRange(request.getMinPrice(), request.getMaxPrice()));
        }
        if (Boolean.TRUE.equals(request.getInStock())) {
            spec = spec.and(CatalogSpecifications.inStock());
        }

        Specification<Product> attrSpec =
                CatalogSpecifications.attributeFilters(request.getAttributes(), categoryAttributes);
        if (attrSpec != null) {
            spec = spec.and(attrSpec);
        }

        Specification<Product> sortSpec =
                CatalogSpecifications.catalogSort(request.getSortBy(), request.getOrder(), categoryAttributes);
        if (sortSpec != null) {
            spec = spec.and(sortSpec);
        }

        int pageNumber = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int pageSize = request.getSize() == null || request.getSize() < 1 ? 20 : request.getSize();

        Page<Product> productsPage = productRepository.findAll(spec, PageRequest.of(pageNumber - 1, pageSize));

        List<Long> productIds =
                productsPage.getContent().stream().map(Product::getId).toList();
        Map<Long, String> primaryImageUrlsMap = fetchPrimaryImageUrlsMap(productIds);

        return productsPage.map(p -> productService.toSummaryResponse(p, primaryImageUrlsMap.get(p.getId())));
    }

    private Map<Long, String> fetchPrimaryImageUrlsMap(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productImageRepository.findPrimaryImagesByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImageRepository.ProductPrimaryImageProjection::getProductId,
                        proj -> resolveImageUrl(proj.getUrl()),
                        (existing, replacement) -> existing));
    }

    private String resolveImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }
        if (awsS3FileService == null) {
            return rawUrl;
        }
        String resolved = awsS3FileService.resolveAccessUrl(rawUrl);
        return resolved != null ? resolved : rawUrl;
    }
}
