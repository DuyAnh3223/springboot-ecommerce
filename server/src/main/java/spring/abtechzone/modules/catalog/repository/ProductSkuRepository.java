package spring.abtechzone.modules.catalog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.catalog.entity.ProductSku;

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

    // Atomic
    @Modifying
    @Query("UPDATE ProductSku s SET s.stock = s.stock - :quantity WHERE s.id = :id AND s.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
