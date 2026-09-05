package spring.abtechzone.modules.inventory.repository;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import spring.abtechzone.modules.inventory.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query(
            value = "select m.id as movementId, m.sku_id as skuId, s.sku as skuCode, "
                    + "m.change_qty as changeQty, m.reason as reason, m.reference_id as referenceId, "
                    + "u.username as createdBy, m.created_at as createdAt "
                    + "from stock_movement m "
                    + "join product_sku s on s.id = m.sku_id "
                    + "left join app_user u on u.id = m.created_by "
                    + "where (:skuId is null or m.sku_id = :skuId) "
                    + "and (:reason is null or m.reason = :reason) "
                    + "order by m.created_at desc, m.id desc",
            countQuery = "select count(*) from stock_movement m "
                    + "where (:skuId is null or m.sku_id = :skuId) "
                    + "and (:reason is null or m.reason = :reason)",
            nativeQuery = true)
    Page<StockMovementHistoryProjection> searchHistory(
            @Param("skuId") Long skuId, @Param("reason") String reason, Pageable pageable);

    interface StockMovementHistoryProjection {
        Long getMovementId();

        Long getSkuId();

        String getSkuCode();

        Integer getChangeQty();

        String getReason();

        String getReferenceId();

        String getCreatedBy();

        OffsetDateTime getCreatedAt();
    }
}
