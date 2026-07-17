package spring.abtechzone.modules.category.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.category.entity.Category;

public class CategorySpecifications {

    public static Specification<Category> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likeValue), cb.like(cb.lower(root.get("slug")), likeValue));
        };
    }

    public static Specification<Category> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return null;
            return cb.equal(root.get("isActive"), active);
        };
    }

    public static Specification<Category> hasParent(Long parentId) {
        return (root, query, cb) -> {
            if (parentId == null) return null;
            if (parentId == 0L) return cb.isNull(root.get("parent"));
            return cb.equal(root.get("parent").get("id"), parentId);
        };
    }
}
