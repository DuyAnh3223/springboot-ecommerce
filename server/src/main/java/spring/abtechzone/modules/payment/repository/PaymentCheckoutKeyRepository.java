package spring.abtechzone.modules.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.payment.entity.PaymentCheckoutKey;

public interface PaymentCheckoutKeyRepository extends JpaRepository<PaymentCheckoutKey, Long> {
    Optional<PaymentCheckoutKey> findByOrderIdAndRequestKey(Long orderId, String requestKey);
}
