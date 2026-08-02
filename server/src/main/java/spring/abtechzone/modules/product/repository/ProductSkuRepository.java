package spring.abtechzone.modules.product.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.product.entity.ProductSku;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long>, JpaSpecificationExecutor<ProductSku> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Override
    @EntityGraph(attributePaths = "product")
    Optional<ProductSku> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "product")
    Page<ProductSku> findAll(Specification<ProductSku> spec, Pageable pageable);

    List<ProductSku> findByProductIdAndDeletedAtIsNull(Long productId);

    @Query("SELECT COUNT(s) FROM ProductSku s WHERE s.product.id = :productId AND s.deletedAt IS NULL")
    long countByProductIdAndDeletedAtIsNull(@Param("productId") Long productId);

    @Query(
            "SELECT COUNT(s) FROM ProductSku s WHERE s.product.id = :productId AND s.deletedAt IS NULL AND s.active = true")
    long countByProductIdAndDeletedAtIsNullAndActiveTrue(@Param("productId") Long productId);

    @Query(
            "SELECT COALESCE(SUM(s.stock), 0) FROM ProductSku s WHERE s.product.id = :productId AND s.deletedAt IS NULL AND s.active = true")
    int sumStockByProductIdAndActiveTrue(@Param("productId") Long productId);

    @Query(
            "SELECT MIN(s.price), MAX(s.price) FROM ProductSku s WHERE s.product.id = :productId AND s.deletedAt IS NULL AND s.active = true")
    Object[] findPriceMinAndMaxByProductIdAndActiveTrue(@Param("productId") Long productId);

    // Atomic
    @Modifying
    @Query("UPDATE ProductSku s SET s.stock = s.stock - :quantity WHERE s.id = :id AND s.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
