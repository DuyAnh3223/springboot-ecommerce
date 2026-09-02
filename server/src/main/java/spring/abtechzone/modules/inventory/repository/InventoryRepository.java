package spring.abtechzone.modules.inventory.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.inventory.entity.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("select i.skuId as skuId, i.onHand as onHand from Inventory i where i.skuId in :skuIds")
    List<SkuOnHandProjection> findOnHandBySkuIds(@Param("skuIds") Collection<Long> skuIds);

    @Query("select i.productSku.product.id as productId, coalesce(sum(i.onHand), 0) as totalOnHand "
            + "from Inventory i "
            + "where i.productSku.product.id in :productIds "
            + "and i.productSku.deletedAt is null "
            + "and i.productSku.active = true "
            + "group by i.productSku.product.id")
    List<ProductOnHandProjection> sumOnHandByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("select s.id from ProductSku s where s.product.id in :productIds "
            + "and s.deletedAt is null and s.active = true")
    List<Long> findActiveSkuIdsByProductIds(@Param("productIds") Collection<Long> productIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Inventory i set i.onHand = i.onHand - :quantity "
            + "where i.skuId = :skuId and i.onHand >= :quantity")
    int decreaseOnHand(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Inventory i set i.onHand = i.onHand + :quantity "
            + "where i.skuId = :skuId "
            + "and i.onHand <= :maxOnHand - :quantity")
    int increaseOnHand(
            @Param("skuId") Long skuId, @Param("quantity") Integer quantity, @Param("maxOnHand") Integer maxOnHand);

    interface SkuOnHandProjection {
        Long getSkuId();

        Integer getOnHand();
    }

    interface ProductOnHandProjection {
        Long getProductId();

        Long getTotalOnHand();
    }
}
