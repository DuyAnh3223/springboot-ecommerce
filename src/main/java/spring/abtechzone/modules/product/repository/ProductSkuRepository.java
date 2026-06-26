package spring.abtechzone.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.product.entity.ProductSku;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}
