package spring.abtechzone.modules.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.cart.entity.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long>, JpaSpecificationExecutor<CartItem> {
    Optional<CartItem> findByCartIdAndProductSkuId(Long cartId, Long productSkuId);

    void deleteAllByCartId(Long cartId);
}
