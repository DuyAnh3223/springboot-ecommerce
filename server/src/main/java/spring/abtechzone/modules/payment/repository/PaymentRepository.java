package spring.abtechzone.modules.payment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import spring.abtechzone.modules.payment.constant.PaymentProvider;
import spring.abtechzone.modules.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByProviderAndMerchantRequestId(PaymentProvider provider, String merchantRequestId);

    @Query("select p.id from Payment p where p.provider = :provider and p.merchantRequestId = :merchantId")
    Optional<Long> findIdByMerchant(
            @Param("provider") PaymentProvider provider, @Param("merchantId") String merchantId);

    @Query(
            "select p.id from Payment p where p.method = spring.abtechzone.modules.order.constant.PaymentMethod.ONLINE "
                    + "and p.nextQueryAt <= :now and (p.leaseUntil is null or p.leaseUntil <= :now) "
                    + "and p.status <> spring.abtechzone.modules.payment.constant.PaymentAttemptStatus.SUCCEEDED "
                    + "and (p.applicationStatus is null or p.applicationStatus <> 'REVIEW_REQUIRED') order by p.nextQueryAt, p.id")
    List<Long> findDue(@Param("now") java.time.OffsetDateTime now, org.springframework.data.domain.Pageable pageable);

    @Query(
            "select p.id from Payment p where p.idempotencyKey = 'initial' "
                    + "and p.paymentDeadline <= :now and p.order.status = spring.abtechzone.modules.order.constant.OrderStatus.PENDING order by p.id")
    List<Long> findExpired(
            @Param("now") java.time.OffsetDateTime now, org.springframework.data.domain.Pageable pageable);

    List<Payment> findByOrderIdInOrderByCreatedAtAscIdAsc(Collection<Long> orderIds);

    List<Payment> findByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);

    Optional<Payment> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);

    Optional<Payment> findByIdAndOrderId(Long paymentId, Long orderId);

    Optional<Payment> findByProviderAndProviderReference(PaymentProvider provider, String providerReference);

    @Query("select p.order.id from Payment p where p.id = :paymentId")
    Optional<Long> findOrderIdByPaymentId(@Param("paymentId") Long paymentId);
}
