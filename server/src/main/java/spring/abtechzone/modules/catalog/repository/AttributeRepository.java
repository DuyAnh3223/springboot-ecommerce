package spring.abtechzone.modules.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import spring.abtechzone.modules.catalog.entity.Attribute;

public interface AttributeRepository extends JpaRepository<Attribute, Long>, JpaSpecificationExecutor<Attribute> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
