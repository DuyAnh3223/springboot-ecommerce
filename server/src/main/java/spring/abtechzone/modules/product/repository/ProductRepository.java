package spring.abtechzone.modules.product.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.product.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Product> findBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // --- Catalog facet queries (replace EntityManager in CatalogService) ---

    @Query("SELECT DISTINCT b.id, b.name, b.slug FROM Product p JOIN p.brand b "
            + "WHERE p.category.id = :categoryId AND p.draft = false AND p.published = true "
            + "AND p.deletedAt IS NULL ORDER BY b.name ASC")
    List<Object[]> findBrandsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT MIN(p.priceMin), MAX(p.priceMax) FROM Product p "
            + "WHERE p.category.id = :categoryId AND p.draft = false AND p.published = true "
            + "AND p.deletedAt IS NULL")
    Object findPriceBoundsByCategoryId(@Param("categoryId") Long categoryId);

    @Query(
            value = "SELECT MIN(CAST(attributes->>:code AS numeric)), MAX(CAST(attributes->>:code AS numeric)) "
                    + "FROM product WHERE category_id = :categoryId AND is_published = true "
                    + "AND is_draft = false AND deleted_at IS NULL",
            nativeQuery = true)
    Object findAttributeNumberBounds(@Param("code") String code, @Param("categoryId") Long categoryId);
}
