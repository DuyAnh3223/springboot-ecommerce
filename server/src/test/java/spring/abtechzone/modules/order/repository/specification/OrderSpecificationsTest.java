package spring.abtechzone.modules.order.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.entity.Order;

class OrderSpecificationsTest {

    private Root<Order> root;
    private CriteriaQuery<?> query;
    private CriteriaBuilder cb;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
    }

    @Test
    void adminSearch_withoutFilters_buildsEmptyConjunction() {
        Predicate conjunction = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(conjunction);

        Predicate result =
                OrderSpecifications.adminSearch(null, null, null, null).toPredicate(root, query, cb);

        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(predicates.capture());
        assertThat(predicates.getValue()).isEmpty();
        assertThat(result).isSameAs(conjunction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminSearch_withAllFilters_buildsExpectedPredicates() {
        Path<String> orderCode = mock(Path.class);
        Path<String> recipientName = mock(Path.class);
        Path<String> phone = mock(Path.class);
        Expression<String> lowerOrderCode = mock(Expression.class);
        Expression<String> lowerRecipientName = mock(Expression.class);
        Expression<String> lowerPhone = mock(Expression.class);
        Path<OrderStatus> statusPath = mock(Path.class);
        Path<Instant> createdAt = mock(Path.class);

        when(root.<String>get("orderCode")).thenReturn(orderCode);
        when(root.<String>get("recipientName")).thenReturn(recipientName);
        when(root.<String>get("phone")).thenReturn(phone);
        when(root.<OrderStatus>get("status")).thenReturn(statusPath);
        when(root.<Instant>get("createdAt")).thenReturn(createdAt);
        when(cb.lower(orderCode)).thenReturn(lowerOrderCode);
        when(cb.lower(recipientName)).thenReturn(lowerRecipientName);
        when(cb.lower(phone)).thenReturn(lowerPhone);

        Predicate orderCodeLike = mock(Predicate.class);
        Predicate recipientNameLike = mock(Predicate.class);
        Predicate phoneLike = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate fromPredicate = mock(Predicate.class);
        Predicate toPredicate = mock(Predicate.class);
        Predicate conjunction = mock(Predicate.class);
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-31T23:59:59Z");

        when(cb.like(lowerOrderCode, "%buyer%")).thenReturn(orderCodeLike);
        when(cb.like(lowerRecipientName, "%buyer%")).thenReturn(recipientNameLike);
        when(cb.like(lowerPhone, "%buyer%")).thenReturn(phoneLike);
        when(cb.or(orderCodeLike, recipientNameLike, phoneLike)).thenReturn(searchPredicate);
        when(cb.equal(statusPath, OrderStatus.PENDING)).thenReturn(statusPredicate);
        when(cb.greaterThanOrEqualTo(createdAt, from)).thenReturn(fromPredicate);
        when(cb.lessThanOrEqualTo(createdAt, to)).thenReturn(toPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(conjunction);

        Predicate result = OrderSpecifications.adminSearch("  BUYER  ", OrderStatus.PENDING, from, to)
                .toPredicate(root, query, cb);

        ArgumentCaptor<Predicate[]> predicates = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(predicates.capture());
        assertThat(predicates.getValue()).containsExactly(searchPredicate, statusPredicate, fromPredicate, toPredicate);
        assertThat(result).isSameAs(conjunction);
    }
}
