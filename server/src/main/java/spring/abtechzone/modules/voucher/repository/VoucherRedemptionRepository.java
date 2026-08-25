package spring.abtechzone.modules.voucher.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;
import spring.abtechzone.modules.voucher.entity.VoucherRedemption;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {
    long countByVoucherIdAndUserIdAndStatus(Long voucherId, UUID userId, VoucherRedemptionStatus status);

    /** Conditionally reverse the single active redemption for an order. */
    @Modifying
    @Query(
            "UPDATE VoucherRedemption r SET r.status = spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus.REVERSED "
                    + "WHERE r.order.id = :orderId AND r.status = spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus.REDEEMED")
    int reverseRedemptionByOrderId(@Param("orderId") Long orderId);
}
