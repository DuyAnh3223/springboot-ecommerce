package spring.abtechzone.modules.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.category.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Boolean existsByName(String name);
}
