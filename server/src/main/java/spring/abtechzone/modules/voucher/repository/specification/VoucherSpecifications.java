package spring.abtechzone.modules.voucher.repository.specification;

import java.time.LocalDateTime;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.voucher.entity.Voucher;

public class VoucherSpecifications {

    public static Specification<Voucher> hasActive(Boolean active) {
        return (root, query, cb) -> {
            if (active == null) return null;
            return cb.equal(root.get("isActive"), active);
        };
    }

    public static Specification<Voucher> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return null;
            LocalDateTime now = LocalDateTime.now();
            if ("expired".equalsIgnoreCase(status)) {
                return cb.and(cb.isNotNull(root.get("endDate")), cb.lessThan(root.get("endDate"), now));
            } else if ("active".equalsIgnoreCase(status)) {
                return cb.and(
                        cb.or(cb.isNull(root.get("startDate")), cb.lessThanOrEqualTo(root.get("startDate"), now)),
                        cb.or(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("endDate"), now)));
            }
            return null;
        };
    }

    public static Specification<Voucher> fetchProductSkus() {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (Long.class != resultType && long.class != resultType) {
                root.fetch("productSkus", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }
}
