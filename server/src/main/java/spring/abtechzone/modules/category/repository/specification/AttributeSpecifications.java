package spring.abtechzone.modules.category.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.category.entity.Attribute;

public class AttributeSpecifications {

    public static Specification<Attribute> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), likeValue),
                    cb.like(cb.lower(root.get("name")), likeValue),
                    cb.like(cb.lower(root.get("unit")), likeValue));
        };
    }
}
