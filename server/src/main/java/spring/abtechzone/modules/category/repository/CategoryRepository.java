package spring.abtechzone.modules.category.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import spring.abtechzone.modules.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    Boolean existsByName(String name);

    Optional<Category> findBySlugAndIsActiveTrue(String slug);
}
