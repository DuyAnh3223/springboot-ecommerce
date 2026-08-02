package spring.abtechzone.modules.product.repository.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;

/**
 * Catalog-specific Specification builders.
 * <p>
 * Reuses {@link ProductSpecifications} for base predicates (isPublished, hasActiveCategory, hasKeyword).
 * Only adds catalog-specific filters: brand, price, stock, JSONB attributes, and sort.
 */
@Slf4j
public class CatalogSpecifications {

    private static final String ATTRIBUTES_FIELD = "attributes";
    private static final String JSONB_EXTRACT_PATH_TEXT = "jsonb_extract_path_text";
    private static final String PRICE_MIN_FIELD = "priceMin";
    private static final String RATING_FIELD = "rating";

    private CatalogSpecifications() {
        // Utility class
    }

    public static Specification<Product> hasBrand(Long brandId) {
        return (root, query, cb) ->
                brandId == null ? null : cb.equal(root.get("brand").get("id"), brandId);
    }

    /**
     * Price containment filter: priceMin >= min AND priceMax <= max.
     * Each bound is independent — null means no constraint on that side.
     */
    public static Specification<Product> priceRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (min != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(PRICE_MIN_FIELD), min));
            }
            if (max != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("priceMax"), max));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("totalStock"), 0);
    }

    /**
     * SKU-level variant attribute filter using EXISTS subquery on ProductSku JSONB.
     */
    public static Specification<Product> hasSkuAttribute(String code, List<String> values) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ProductSku> skuRoot = subquery.from(ProductSku.class);

            Expression<String> skuValueExpr =
                    cb.function(JSONB_EXTRACT_PATH_TEXT, String.class, skuRoot.get(ATTRIBUTES_FIELD), cb.literal(code));

            subquery.select(skuRoot.get("id"))
                    .where(
                            cb.equal(skuRoot.get("product"), root),
                            cb.isNull(skuRoot.get("deletedAt")),
                            cb.equal(skuRoot.get("active"), true),
                            skuValueExpr.in(values));

            return cb.exists(subquery);
        };
    }

    /**
     * Product-level JSONB attribute filter for STRING / ENUM / BOOLEAN types.
     */
    public static Specification<Product> hasProductAttribute(String code, List<String> values) {
        return (root, query, cb) -> {
            Expression<String> strExpr =
                    cb.function(JSONB_EXTRACT_PATH_TEXT, String.class, root.get(ATTRIBUTES_FIELD), cb.literal(code));
            return strExpr.in(values);
        };
    }

    /**
     * Product-level JSONB NUMBER attribute filter.
     * Supports value formats: "min:100", "max:500", or exact "250".
     * Multiple values are AND-ed (e.g. min:100 AND max:500 = range).
     */
    public static Specification<Product> hasProductNumberAttribute(String code, List<String> values) {
        return (root, query, cb) -> {
            Expression<String> strExpr =
                    cb.function(JSONB_EXTRACT_PATH_TEXT, String.class, root.get(ATTRIBUTES_FIELD), cb.literal(code));
            Expression<BigDecimal> numExpr =
                    cb.function("to_number", BigDecimal.class, strExpr, cb.literal("999999999"));

            var predicates = new ArrayList<Predicate>();
            for (String val : values) {
                parseAndAddNumberPredicate(cb, numExpr, val, predicates);
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void parseAndAddNumberPredicate(
            CriteriaBuilder cb, Expression<BigDecimal> numExpr, String val, List<Predicate> predicates) {
        if (val.startsWith("min:")) {
            parseMinNumberPredicate(cb, numExpr, val, predicates);
        } else if (val.startsWith("max:")) {
            parseMaxNumberPredicate(cb, numExpr, val, predicates);
        } else {
            parseExactNumberPredicate(cb, numExpr, val, predicates);
        }
    }

    private static void parseMinNumberPredicate(
            CriteriaBuilder cb, Expression<BigDecimal> numExpr, String val, List<Predicate> predicates) {
        try {
            BigDecimal min = new BigDecimal(val.substring(4));
            predicates.add(cb.greaterThanOrEqualTo(numExpr, min));
        } catch (NumberFormatException e) {
            log.debug("Skipping invalid min number filter parameter value: {}", val, e);
        }
    }

    private static void parseMaxNumberPredicate(
            CriteriaBuilder cb, Expression<BigDecimal> numExpr, String val, List<Predicate> predicates) {
        try {
            BigDecimal max = new BigDecimal(val.substring(4));
            predicates.add(cb.lessThanOrEqualTo(numExpr, max));
        } catch (NumberFormatException e) {
            log.debug("Skipping invalid max number filter parameter value: {}", val, e);
        }
    }

    private static void parseExactNumberPredicate(
            CriteriaBuilder cb, Expression<BigDecimal> numExpr, String val, List<Predicate> predicates) {
        try {
            BigDecimal exact = new BigDecimal(val);
            predicates.add(cb.equal(numExpr, exact));
        } catch (NumberFormatException e) {
            log.debug("Skipping invalid exact number filter parameter value: {}", val, e);
        }
    }

    /**
     * Builds dynamic attribute filter specifications from the request attributes map.
     * Iterates category attributes, checks filterability, and dispatches to the correct
     * SKU-level or product-level specification.
     */
    public static Specification<Product> attributeFilters(
            Map<String, List<String>> requestAttributes, List<CategoryAttribute> categoryAttributes) {
        if (requestAttributes == null || requestAttributes.isEmpty()) {
            return null;
        }

        Map<String, CategoryAttribute> caMap = categoryAttributes.stream()
                .collect(Collectors.toMap(
                        ca -> ca.getAttribute().getCode(), ca -> ca, (existing, replacement) -> existing));

        Specification<Product> combined = null;
        for (Map.Entry<String, List<String>> entry : requestAttributes.entrySet()) {
            String code = entry.getKey();
            List<String> values = entry.getValue();
            CategoryAttribute ca = (code != null && values != null && !values.isEmpty()) ? caMap.get(code) : null;

            if (ca != null && Boolean.TRUE.equals(ca.getIsFilterable())) {
                Specification<Product> filterSpec = buildSingleAttributeFilter(code, values, ca);
                combined = (combined == null) ? Specification.where(filterSpec) : combined.and(filterSpec);
            }
        }
        return combined;
    }

    private static Specification<Product> buildSingleAttributeFilter(
            String code, List<String> values, CategoryAttribute ca) {
        if (Boolean.TRUE.equals(ca.getIsVariantDefining())) {
            return hasSkuAttribute(code, values);
        }
        if ("NUMBER".equalsIgnoreCase(ca.getAttribute().getDataType())) {
            return hasProductNumberAttribute(code, values);
        }
        return hasProductAttribute(code, values);
    }

    /**
     * Sets ORDER BY on the CriteriaQuery inside toPredicate().
     * <p>
     * Guard: Skips for count queries (resultType == Long.Class) to avoid
     * invalid ORDER BY on aggregate queries
     */
    public static Specification<Product> catalogSort(
            String sortBy, String order, List<CategoryAttribute> categoryAttributes) {
        return (root, query, cb) -> {
            // Guard: only set orderBy for Product data query (not count query or other projections)
            Class<?> resultType = query.getResultType();
            if (resultType == null || !Product.class.isAssignableFrom(resultType)) {
                return null;
            }

            boolean isAsc = "asc".equalsIgnoreCase(order);
            String sortField = sortBy == null ? "" : sortBy.trim();

            Order orderExpr = buildDynamicOrder(cb, root, sortField, isAsc, categoryAttributes);
            query.orderBy(orderExpr);

            return null; // no WHERE predicate, only ORDER BY side effect
        };
    }

    private static Order buildDynamicOrder(
            CriteriaBuilder cb,
            Root<Product> root,
            String sortBy,
            boolean isAsc,
            List<CategoryAttribute> categoryAttributes) {

        Optional<CategoryAttribute> sortableCa = categoryAttributes.stream()
                .filter(ca -> Boolean.TRUE.equals(ca.getIsSortable())
                        && ca.getAttribute().getCode().equalsIgnoreCase(sortBy))
                .findFirst();

        if (sortableCa.isPresent()) {
            return buildCategoryAttributeOrder(cb, root, sortableCa.get(), isAsc);
        }

        return buildDefaultOrder(cb, root, sortBy, isAsc);
    }

    private static Order buildCategoryAttributeOrder(
            CriteriaBuilder cb, Root<Product> root, CategoryAttribute ca, boolean isAsc) {
        String code = ca.getAttribute().getCode();
        Expression<String> strExpr =
                cb.function(JSONB_EXTRACT_PATH_TEXT, String.class, root.get(ATTRIBUTES_FIELD), cb.literal(code));

        if ("NUMBER".equalsIgnoreCase(ca.getAttribute().getDataType())) {
            Expression<BigDecimal> numExpr =
                    cb.function("to_number", BigDecimal.class, strExpr, cb.literal("999999999"));
            return isAsc ? cb.asc(numExpr) : cb.desc(numExpr);
        }
        return isAsc ? cb.asc(strExpr) : cb.desc(strExpr);
    }

    private static Order buildDefaultOrder(CriteriaBuilder cb, Root<Product> root, String sortBy, boolean isAsc) {
        return switch (sortBy.toLowerCase()) {
            case "price", "pricemin", "price_min" ->
                isAsc ? cb.asc(root.get(PRICE_MIN_FIELD)) : cb.desc(root.get(PRICE_MIN_FIELD));
            case RATING_FIELD -> isAsc ? cb.asc(root.get(RATING_FIELD)) : cb.desc(root.get(RATING_FIELD));
            case "updatedat", "updated_at" -> isAsc ? cb.asc(root.get("updatedAt")) : cb.desc(root.get("updatedAt"));
            default -> isAsc ? cb.asc(root.get("name")) : cb.desc(root.get("name"));
        };
    }
}
