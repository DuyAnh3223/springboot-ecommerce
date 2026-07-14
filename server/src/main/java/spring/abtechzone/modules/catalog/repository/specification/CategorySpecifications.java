package spring.abtechzone.modules.catalog.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.catalog.entity.Category;

public class CategorySpecifications {

    public static Specification<Category> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likeValue),
                    cb.like(cb.lower(root.get("slug")), likeValue));
        };
    }

    public static Specification<Category> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return null;
            return cb.equal(root.get("isActive"), active);
        };
    }

    /**
     * Filter: parentId:
     * - null  → No filter -> Return all
     * - 0L    → get root category (parent IS NULL)
     * - n > 0 → get child of category has id = n
     */
    public static Specification<Category> hasParent(Long parentId) {
        return (root, query, cb) -> {
            if (parentId == null) return null;
            if (parentId == 0L) return cb.isNull(root.get("parent"));
            return cb.equal(root.get("parent").get("id"), parentId);
        };
    }
}
