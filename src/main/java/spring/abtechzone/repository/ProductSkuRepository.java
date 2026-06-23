package spring.abtechzone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import spring.abtechzone.entity.ProductSku;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {}
