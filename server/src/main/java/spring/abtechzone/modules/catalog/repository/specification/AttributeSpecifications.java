package spring.abtechzone.modules.catalog.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.catalog.entity.Attribute;

public class AttributeSpecifications {
    public static Specification<Attribute> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), likeValue),
                    cb.like(cb.lower(root.get("name")), likeValue),
                    cb.like(cb.lower(root.get("unit")), likeValue));
        };
    }

    public static Specification<Attribute> isFilterable(Boolean isFilterable) {
        return (root, query, cb) -> {
            if (isFilterable == null) return null;
            return cb.equal(root.get("isFilterable"), isFilterable);
        };
    }

    public static Specification<Attribute> isVariantDefining(Boolean isVariantDefining) {
        return (root, query, cb) -> {
            if (isVariantDefining == null) return null;
            return cb.equal(root.get("isVariantDefining"), isVariantDefining);
        };
    }

    public static Specification<Attribute> isCompatibilityKey(Boolean isCompatibilityKey) {
        return (root, query, cb) -> {
            if (isCompatibilityKey == null) return null;
            return cb.equal(root.get("isCompatibilityKey"), isCompatibilityKey);
        };
    }

    public static Specification<Attribute> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return null;
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }
}
