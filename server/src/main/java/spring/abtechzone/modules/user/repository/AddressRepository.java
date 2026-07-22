package spring.abtechzone.modules.user.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.user.entity.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID>, JpaSpecificationExecutor<Address> {
    List<Address> findByUserId(UUID userId);

    boolean existsByUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void unsetDefaultAddressesByUserId(@Param("userId") UUID userId);
}
