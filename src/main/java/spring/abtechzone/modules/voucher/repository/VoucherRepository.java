package spring.abtechzone.modules.voucher.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.voucher.entity.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {}
