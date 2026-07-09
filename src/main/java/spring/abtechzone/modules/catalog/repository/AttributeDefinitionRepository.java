package spring.abtechzone.modules.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.catalog.entity.AttributeDefinition;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {
    List<AttributeDefinition> findByCategoryId(Long categoryId);
}
