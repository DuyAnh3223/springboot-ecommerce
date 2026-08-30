package spring.abtechzone.modules.cart.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
			select distinct c from Cart c
			left join fetch c.items i
			left join fetch i.productSku sku
			left join fetch sku.product
			where c.user.id = :userId and c.status = :status
			""")
    Optional<Cart> findByUserIdAndStatusForUpdate(@Param("userId") UUID userId, @Param("status") CartStatus status);
}
