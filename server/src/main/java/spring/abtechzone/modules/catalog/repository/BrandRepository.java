package spring.abtechzone.modules.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.catalog.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Boolean existsByName(String name);
}
