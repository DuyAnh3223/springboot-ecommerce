package spring.abtechzone.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.entity.Voucher;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {}
