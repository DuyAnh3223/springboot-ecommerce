package spring.abtechzone.modules.voucher.repository;

import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.voucher.entity.Voucher;

public interface VoucherRepository
        extends JpaRepository<Voucher, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Voucher> {
    boolean existsByCode(@NotBlank String code);

    List<Voucher> findAllByIsActiveTrue();

    Optional<Voucher> findByCode(@NotBlank String code);
}
