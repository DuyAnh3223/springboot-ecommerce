package spring.abtechzone.modules.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.dto.ResolvedShippingRoute;
import spring.abtechzone.modules.shipment.service.ShippingLocationService;
import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.request.AddressSearchRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.entity.Address;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.mapper.AddressMapper;
import spring.abtechzone.modules.user.repository.AddressRepository;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressService {
    AddressRepository addressRepository;
    AddressMapper addressMapper;
    UserService userService;
    ShippingLocationService shippingLocationService;

    public AddressResponse create(AddressRequest request) {
        User currentUser = userService.getCurrentUser();
        if (shippingLocationService != null) {
            validateAddressFields(request, true);
        }
        Address address = addressMapper.toAddress(request);
        applyCanonicalRoute(request, address, true);
        address.setUser(currentUser);

        boolean hasDefault = addressRepository.existsByUserIdAndIsDefaultTrue(currentUser.getId());
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.unsetDefaultAddressesByUserId(currentUser.getId());
        } else if (!hasDefault) {
            address.setIsDefault(true);
        }

        return addressMapper.toAddressResponse(addressRepository.save(address));
    }

    public Page<AddressResponse> getAddresses(AddressSearchRequest request) {
        User currentUser = userService.getCurrentUser();
        Specification<Address> spec = Specification.where(hasKeyWord(request.getSearch()))
                .and((root, query, cb) -> cb.equal(root.get("user").get("id"), currentUser.getId()));
        return addressRepository.findAll(spec, request.toPageable()).map(addressMapper::toAddressResponse);
    }

    public AddressResponse getAddress(UUID addressId) {
        User currentUser = userService.getCurrentUser();
        Address address = findUserAddressWithOwnershipCheck(addressId);

        if (address.getUser() == null || !address.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return addressMapper.toAddressResponse(address);
    }

    public AddressResponse updateAddress(UUID addressId, AddressRequest request) {
        User currentUser = userService.getCurrentUser();
        Address address = findUserAddressWithOwnershipCheck(addressId);
        if (shippingLocationService != null) {
            validateAddressFields(request, false);
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.unsetDefaultAddressesByUserId(currentUser.getId());
        }

        addressMapper.updateAddress(address, request);
        applyCanonicalRoute(request, address, false);
        return addressMapper.toAddressResponse(addressRepository.save(address));
    }

    public void deleteAddress(UUID addressId) {
        Address address = findUserAddressWithOwnershipCheck(addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        UUID userId = address.getUser().getId();

        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserId(userId);
            if (!remaining.isEmpty()) {
                Address first = remaining.get(0);
                first.setIsDefault(true);
                addressRepository.save(first);
            }
        }
    }

    private Address findUserAddressWithOwnershipCheck(UUID addressId) {
        User currentUser = userService.getCurrentUser();
        Address address =
                addressRepository.findById(addressId).orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (address.getUser() == null || !address.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED); // 403 Forbidden
        }
        return address;
    }

    private static Specification<Address> hasKeyWord(String keyword) {
        return ((root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("province")), likeValue),
                    cb.like(cb.lower(root.get("district")), likeValue),
                    cb.like(cb.lower(root.get("ward")), likeValue),
                    cb.like(cb.lower(root.get("street")), likeValue),
                    cb.like(cb.lower(root.get("country")), likeValue));
        });
    }

    private void applyCanonicalRoute(AddressRequest request, Address address, boolean required) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        if (request.getCountry() != null
                && !request.getCountry().isBlank()
                && !"VN".equalsIgnoreCase(request.getCountry().trim())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        boolean hasAnyRoute = request.getGhnProvinceId() != null
                || request.getGhnDistrictId() != null
                || request.getGhnWardCode() != null
                || request.getProvince() != null
                || request.getDistrict() != null
                || request.getWard() != null;
        if (!required && !hasAnyRoute) {
            return;
        }
        if (shippingLocationService == null) {
            return;
        }
        if (request.getGhnProvinceId() == null
                || request.getGhnDistrictId() == null
                || request.getGhnWardCode() == null
                || request.getGhnProvinceId() <= 0
                || request.getGhnDistrictId() <= 0
                || request.getGhnWardCode().isBlank()) {
            throw new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID);
        }
        ResolvedShippingRoute route = shippingLocationService.resolveRoute(
                request.getGhnProvinceId(), request.getGhnDistrictId(), request.getGhnWardCode());
        address.setGhnProvinceId(route.provinceId());
        address.setGhnDistrictId(route.districtId());
        address.setGhnWardCode(route.wardCode());
        address.setProvince(route.province());
        address.setDistrict(route.district());
        address.setWard(route.ward());
    }

    private void validateAddressFields(AddressRequest request, boolean required) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        validateText(request.getRecipientName(), required);
        validateText(request.getPhone(), required);
        validateText(request.getProvince(), required);
        validateText(request.getDistrict(), required);
        validateText(request.getWard(), required);
        validateText(request.getStreet(), required);
    }

    private void validateText(String value, boolean required) {
        if ((required || value != null) && (value == null || value.isBlank())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }
}
