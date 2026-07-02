package spring.abtechzone.modules.catalog.repository.specification;

import java.math.BigDecimal;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.catalog.entity.Product;
import spring.abtechzone.modules.catalog.entity.ProductSku;

public class ProductSkuSpecifications {
    public static Specification<ProductSku> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;

            Join<ProductSku, Product> product = root.join("product", JoinType.LEFT);
            String likeValue = "%" + keyword.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("sku")), likeValue), cb.like(cb.lower(product.get("name")), likeValue));
        };
    }

    public static Specification<ProductSku> hasProductId(Long productId) {
        return (root, query, cb) ->
                productId == null ? null : cb.equal(root.get("product").get("id"), productId);
    }

    public static Specification<ProductSku> hasMinPrice(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<ProductSku> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
