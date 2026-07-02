package spring.abtechzone.modules.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.catalog.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Boolean existsByName(Category category);
}
