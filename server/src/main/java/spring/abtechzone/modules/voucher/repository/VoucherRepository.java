package spring.abtechzone.modules.voucher.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import spring.abtechzone.modules.voucher.entity.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {
    boolean existsByCode(@NotBlank String code);

    List<Voucher> findAllByIsActiveTrue();

    Optional<Voucher> findByCode(@NotBlank String code);

    @Modifying
    @Query(
            value = "UPDATE voucher " + "SET used_count = COALESCE(used_count, 0) + 1 "
                    + "WHERE id = :id "
                    + "  AND (max_uses IS NULL OR COALESCE(used_count, 0) < max_uses) "
                    + "  AND (max_per_user IS NULL OR ( "
                    + "      SELECT COUNT(*) FROM voucher_user vu WHERE vu.voucher_id = :id AND vu.user_id = :userId "
                    + "  ) < max_per_user)",
            nativeQuery = true)
    int increaseUsedCount(@Param("id") Long id, @Param("userId") UUID userId);

    @Modifying
    @Query(value = "INSERT INTO voucher_user (voucher_id, user_id) VALUES (:voucherId, :userId)", nativeQuery = true)
    void insertVoucherUser(@Param("voucherId") Long voucherId, @Param("userId") UUID userId);
}
