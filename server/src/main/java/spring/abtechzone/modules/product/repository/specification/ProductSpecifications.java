package spring.abtechzone.modules.product.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.product.entity.Product;

public class ProductSpecifications {

    private static final String DRAFT = "draft";
    private static final String PUBLISHED = "published";

    private ProductSpecifications() {
        // Utility class
    }

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeValue));
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> hasActiveCategory(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.and(
                    cb.equal(root.get("category").get("id"), categoryId),
                    cb.equal(root.get("category").get("isActive"), true));
        };
    }

    public static Specification<Product> isPublished() {
        return (root, query, cb) -> cb.and(cb.equal(root.get(DRAFT), false), cb.equal(root.get(PUBLISHED), true));
    }

    public static Specification<Product> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
                return null;
            }
            if (DRAFT.equalsIgnoreCase(status)) {
                return cb.equal(root.get(DRAFT), true);
            }
            if (PUBLISHED.equalsIgnoreCase(status)) {
                return cb.and(cb.equal(root.get(DRAFT), false), cb.equal(root.get(PUBLISHED), true));
            }
            return null;
        };
    }
}
