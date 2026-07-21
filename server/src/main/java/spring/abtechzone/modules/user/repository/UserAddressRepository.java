package spring.abtechzone.modules.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.user.entity.UserAddress;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID>, JpaSpecificationExecutor<UserAddress> {
    List<UserAddress> findByUserId(UUID userId);

    boolean existsByUserIdAndIsDefaultTrue(UUID userId);
}
