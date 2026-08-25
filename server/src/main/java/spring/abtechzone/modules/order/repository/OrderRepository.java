package spring.abtechzone.modules.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.order.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    /**
     * Load an order by code with a pessimistic write lock (R-C05-05).
     * Mutations lock the order row first so the single successful status
     * transition acts as the exact-once guard (ADR-003). Items are fetched
     * eagerly so cancellation compensation can restore stock from the
     * persisted OrderItem quantities.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "items.sku"})
    @Query("SELECT o FROM Order o WHERE o.orderCode = :orderCode")
    Optional<Order> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

    /** Owner-aware detail read for customer APIs (R-C05-02). */
    Optional<Order> findByOrderCodeAndUserId(String orderCode, UUID userId);

    /** Customer order list: newest first, optional status filter, pageable. */
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(
            UUID userId, spring.abtechzone.modules.order.constant.OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.sku", "items.sku.product"})
    Optional<Order> findWithItemsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"items", "items.sku", "items.sku.product"})
    Optional<Order> findWithItemsByOrderCodeAndUserId(String orderCode, UUID userId);
}
