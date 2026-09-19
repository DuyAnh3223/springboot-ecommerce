package spring.abtechzone.modules.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.shipment.dto.ShippingAddressData;
import spring.abtechzone.modules.user.entity.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID>, JpaSpecificationExecutor<Address> {
    // Scalar projection avoids seeding OSIV with a pre-lock managed Address.
    @Query("""
			select new spring.abtechzone.modules.shipment.dto.ShippingAddressData(
				a.id, a.recipientName, a.phone, a.province, a.district, a.ward, a.street,
				a.ghnProvinceId, a.ghnDistrictId, a.ghnWardCode)
			from Address a where a.id = :id and a.user.id = :userId
			""")
    Optional<ShippingAddressData> findShippingAddress(@Param("id") UUID id, @Param("userId") UUID userId);

    List<Address> findByUserId(UUID userId);

    boolean existsByUserIdAndIsDefaultTrue(UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void unsetDefaultAddressesByUserId(@Param("userId") UUID userId);
}
