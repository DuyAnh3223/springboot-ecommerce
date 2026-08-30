package spring.abtechzone.modules.order.repository.specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.entity.Order;

public final class OrderSpecifications {

    private static final String CREATED_AT = "createdAt";

    private OrderSpecifications() {}

    public static Specification<Order> adminSearch(
            String search, OrderStatus status, Instant fromDate, Instant toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderCode")), pattern),
                        cb.like(cb.lower(root.get("recipientName")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(CREATED_AT), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(CREATED_AT), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
