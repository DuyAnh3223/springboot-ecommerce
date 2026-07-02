package spring.abtechzone.modules.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.catalog.entity.AttributeDefinition;

import java.util.List;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    List<AttributeDefinition> findByCategoryId(Long categoryId);
}
