package spring.abtechzone.modules.voucher.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;
import spring.abtechzone.modules.voucher.entity.VoucherRedemption;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long> {
    long countByVoucherIdAndUserIdAndStatus(Long voucherId, UUID userId, VoucherRedemptionStatus status);
}
