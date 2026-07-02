package spring.abtechzone.modules.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.inventory.entity.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {}
