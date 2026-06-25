package spring.abtechzone.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.entity.Product;

public class ProductSpecifications {
    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeValue));
        };
    }

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) ->
                (category == null || category.isBlank()) ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> isPublished() {
        return (root, query, cb) -> {
            return cb.and(cb.equal(root.get("isDraft"), false), cb.equal(root.get("isPublished"), true));
        };
    }
}
