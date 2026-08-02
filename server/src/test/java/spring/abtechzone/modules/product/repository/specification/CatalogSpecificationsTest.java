package spring.abtechzone.modules.product.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import spring.abtechzone.modules.category.entity.Attribute;
import spring.abtechzone.modules.category.entity.CategoryAttribute;
import spring.abtechzone.modules.product.entity.Product;

class CatalogSpecificationsTest {

    private Root<Product> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
    }

    @Nested
    @DisplayName("hasBrand specification tests")
    class HasBrandTests {

        @Test
        @DisplayName("Returns null predicate when brandId is null")
        void hasBrand_null_returnsNull() {
            var spec = CatalogSpecifications.hasBrand(null);
            Predicate predicate = spec.toPredicate(root, query, cb);
            assertThat(predicate).isNull();
        }

        @Test
        @DisplayName("Returns equal predicate when brandId is present")
        @SuppressWarnings("unchecked")
        void hasBrand_valid_returnsEqualPredicate() {
            Path<Object> brandPath = mock(Path.class);
            Path<Object> idPath = mock(Path.class);
            Predicate expectedPredicate = mock(Predicate.class);

            when(root.get("brand")).thenReturn(brandPath);
            when(brandPath.get("id")).thenReturn(idPath);
            when(cb.equal(idPath, 100L)).thenReturn(expectedPredicate);

            var spec = CatalogSpecifications.hasBrand(100L);
            Predicate predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isEqualTo(expectedPredicate);
        }
    }

    @Nested
    @DisplayName("priceRange specification tests")
    class PriceRangeTests {

        @Test
        @DisplayName("Returns null predicate when both min and max are null")
        void priceRange_bothNull_returnsNull() {
            var spec = CatalogSpecifications.priceRange(null, null);
            Predicate predicate = spec.toPredicate(root, query, cb);
            assertThat(predicate).isNull();
        }

        @Test
        @DisplayName("Returns priceMin >= min predicate when only min is provided")
        @SuppressWarnings("unchecked")
        void priceRange_minOnly_returnsGtePredicate() {
            Path<BigDecimal> priceMinPath = mock(Path.class);
            Predicate expectedPredicate = mock(Predicate.class);

            when(root.<BigDecimal>get("priceMin")).thenReturn(priceMinPath);
            when(cb.greaterThanOrEqualTo(priceMinPath, new BigDecimal("1000"))).thenReturn(expectedPredicate);
            when(cb.and(any(Predicate[].class))).thenAnswer(invocation -> expectedPredicate);

            var spec = CatalogSpecifications.priceRange(new BigDecimal("1000"), null);
            Predicate predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isNotNull();
        }
    }

    @Nested
    @DisplayName("attributeFilters specification tests")
    class AttributeFiltersTests {

        @Test
        @DisplayName("Returns null specification when request attributes map is empty")
        void attributeFilters_empty_returnsNull() {
            var spec = CatalogSpecifications.attributeFilters(Map.of(), List.of());
            assertThat(spec).isNull();
        }

        @Test
        @DisplayName("Skips non-filterable attributes")
        void attributeFilters_nonFilterable_skipped() {
            Attribute attr = new Attribute();
            attr.setCode("COLOR");
            attr.setDataType("STRING");

            CategoryAttribute ca = new CategoryAttribute();
            ca.setAttribute(attr);
            ca.setIsFilterable(false);

            var spec = CatalogSpecifications.attributeFilters(Map.of("COLOR", List.of("Red")), List.of(ca));

            assertThat(spec).isNull();
        }
    }

    @Nested
    @DisplayName("catalogSort specification tests")
    class CatalogSortTests {

        @Test
        @DisplayName("Count query guard: returns null and does not set orderBy when resultType is Long")
        void catalogSort_countQuery_skipsOrderBy() {
            doReturn(Long.class).when(query).getResultType();

            var spec = CatalogSpecifications.catalogSort("name", "asc", List.of());
            Predicate predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isNull();
            verify(query, never()).orderBy(any(Order.class));
        }

        @Test
        @DisplayName("Data query: applies orderBy when resultType is Product.class")
        @SuppressWarnings("unchecked")
        void catalogSort_dataQuery_appliesOrderBy() {
            doReturn(Product.class).when(query).getResultType();
            Path<Object> namePath = mock(Path.class);
            Order expectedOrder = mock(Order.class);

            when(root.get("name")).thenReturn(namePath);
            when(cb.asc(namePath)).thenReturn(expectedOrder);

            var spec = CatalogSpecifications.catalogSort("name", "asc", List.of());
            Predicate predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isNull();
            verify(query).orderBy(expectedOrder);
        }
    }
}
