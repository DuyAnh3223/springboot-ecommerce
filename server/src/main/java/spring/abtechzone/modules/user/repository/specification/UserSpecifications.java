package spring.abtechzone.modules.user.repository.specification;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.user.entity.User;

public class UserSpecifications {

    public static Specification<User> hasKeyword(String keyword) {
        return ((root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), likeValue),
                    cb.like(cb.lower(root.get("email")), likeValue),
                    cb.like(cb.lower(root.get("firstName")), likeValue),
                    cb.like(cb.lower(root.get("lastName")), likeValue),
                    cb.like(cb.lower(root.get("phone")), likeValue));
        });
    }

    public static Specification<User> isActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return null;
            return cb.equal(root.get("isActive"), active);
        };
    }
}
