package spring.abtechzone.modules.catalog.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.catalog.dto.request.CatalogSearchRequest;
import spring.abtechzone.modules.catalog.dto.response.CatalogProductDetailResponse;
import spring.abtechzone.modules.catalog.dto.response.CategoryFacetResponse;
import spring.abtechzone.modules.catalog.service.CatalogService;
import spring.abtechzone.modules.product.dto.response.ProductResponse;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogController {

    CatalogService catalogService;

    @GetMapping("/products/{slug}")
    public ApiResult<CatalogProductDetailResponse> getProductDetail(@PathVariable String slug) {
        return ApiResult.<CatalogProductDetailResponse>builder()
                .result(catalogService.getProductDetail(slug))
                .build();
    }

    @GetMapping("/category/{slug}/facets")
    public ApiResult<CategoryFacetResponse> getCategoryFacets(@PathVariable String slug) {
        CategoryFacetResponse facets = catalogService.getCategoryFacets(slug);
        return ApiResult.<CategoryFacetResponse>builder().result(facets).build();
    }

    @GetMapping("/category/{slug}/products")
    public ApiResult<Page<ProductResponse>> getCatalogProducts(
            @PathVariable String slug,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            HttpServletRequest request) {

        Map<String, List<String>> attributeFilters = extractAttributeFilters(request);

        CatalogSearchRequest searchRequest = CatalogSearchRequest.builder()
                .search(search)
                .brandId(brandId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStock(inStock)
                .attributes(attributeFilters)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .order(order)
                .build();

        Page<ProductResponse> products = catalogService.getCatalogProducts(slug, searchRequest);

        return ApiResult.<Page<ProductResponse>>builder().result(products).build();
    }

    private Map<String, List<String>> extractAttributeFilters(HttpServletRequest request) {
        Map<String, List<String>> filters = new HashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            if (paramName.startsWith("attr_")) {
                String attrCode = paramName.substring(5);
                List<String> values = parseAttributeValues(entry.getValue());
                if (!values.isEmpty()) {
                    filters.put(attrCode, values);
                }
            }
        }
        return filters;
    }

    private List<String> parseAttributeValues(String[] paramValues) {
        if (paramValues == null) {
            return List.of();
        }
        return Arrays.stream(paramValues)
                .filter(val -> val != null && !val.isBlank())
                .flatMap(val -> Arrays.stream(val.split(",")))
                .map(String::trim)
                .filter(val -> !val.isBlank())
                .toList();
    }
}
