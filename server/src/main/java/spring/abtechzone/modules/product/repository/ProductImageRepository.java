package spring.abtechzone.modules.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.product.entity.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findBySkuIdOrderBySortOrderAsc(Long skuId);

    void deleteBySkuId(Long skuId);

    interface ProductPrimaryImageProjection {
        Long getProductId();

        String getUrl();
    }

    @Query("""
		SELECT i.product.id AS productId, i.url AS url
		FROM ProductImage i
		WHERE i.product.id IN :productIds
		AND i.primary = true
		ORDER BY i.sku.id ASC, i.sortOrder ASC, i.id ASC
	""")
    List<ProductPrimaryImageProjection> findPrimaryImagesByProductIds(@Param("productIds") List<Long> productIds);
}
