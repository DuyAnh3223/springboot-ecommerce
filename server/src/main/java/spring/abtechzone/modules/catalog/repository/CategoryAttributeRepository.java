package spring.abtechzone.modules.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import spring.abtechzone.modules.catalog.entity.CategoryAttribute;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {

    boolean existsByCategory_IdAndAttribute_Id(Long categoryId, Long attributeId);

    @Query(
            "SELECT ca FROM CategoryAttribute ca JOIN FETCH ca.attribute WHERE ca.category.id = :categoryId ORDER BY ca.sortOrder ASC")
    List<CategoryAttribute> findByCategoryIdWithAttribute(@Param("categoryId") Long categoryId);

    void deleteByCategory_IdAndAttribute_Id(Long categoryId, Long attributeId);
}
