package spring.abtechzone.modules.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query(value = "SELECT COUNT(*) > 0 FROM product WHERE slug = :slug", nativeQuery = true)
    boolean existsBySlugIncludingDeleted(@Param("slug") String slug);

    @Query(value = "SELECT COUNT(*) > 0 FROM product WHERE slug = :slug AND id != :id", nativeQuery = true)
    boolean existsBySlugAndIdNotIncludingDeleted(@Param("slug") String slug, @Param("id") Long id);
}
