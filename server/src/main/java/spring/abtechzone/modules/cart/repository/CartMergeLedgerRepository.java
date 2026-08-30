package spring.abtechzone.modules.cart.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.cart.entity.CartMergeLedger;

@Repository
public interface CartMergeLedgerRepository extends JpaRepository<CartMergeLedger, Long> {

    Optional<CartMergeLedger> findByUserIdAndMergeId(UUID userId, UUID mergeId);
}
