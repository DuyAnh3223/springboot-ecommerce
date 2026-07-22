package spring.abtechzone.modules.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(java.util.UUID userId);

    Optional<Cart> findByUserIdAndStatus(java.util.UUID userId, CartStatus status);
}
