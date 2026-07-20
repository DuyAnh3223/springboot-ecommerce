package spring.abtechzone.modules.user.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.request.AddressSearchRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.mapper.AddressMapper;
import spring.abtechzone.modules.user.repository.UserAddressRepository;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressService {
    UserAddressRepository userAddressRepository;
    AddressMapper addressMapper;
    UserService userService;

    public AddressResponse create(AddressRequest request) {
        User currentUser = userService.getCurrentUser();
        UserAddress address = addressMapper.toAddress(request);
        address.setUser(currentUser);

        if (address.isDefault() && userAddressRepository.existsByUserIdAndIsDefaultTrue(currentUser.getId())) {
            address.setDefault(false);
        }

        return addressMapper.toAddressResponse(userAddressRepository.save(address));
    }

    public Page<AddressResponse> getAddresses(AddressSearchRequest request) {
        User currentUser = userService.getCurrentUser();
        Specification<UserAddress> spec = Specification.where(hasKeyWord(request.getSearch()))
                .and((root, query, cb) -> cb.equal(root.get("user").get("id"), currentUser.getId()));
        return userAddressRepository.findAll(spec, request.toPageable()).map(addressMapper::toAddressResponse);

    }

    public AddressResponse getAddress(UUID addressId) {
        User currentUser = userService.getCurrentUser();
        UserAddress address = findUserAddressWithOwnershipCheck(addressId);

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return addressMapper.toAddressResponse(address);
    }

    public AddressResponse updateAddress(UUID addressId, AddressRequest request) {
        UserAddress address = findUserAddressWithOwnershipCheck(addressId);
        addressMapper.updateAddress(address, request);
        return addressMapper.toAddressResponse(userAddressRepository.save(address));
    }


    public void deleteAddress(UUID addressId) {
        UserAddress address = findUserAddressWithOwnershipCheck(addressId);
        userAddressRepository.delete(address);
    }


    private UserAddress findUserAddressWithOwnershipCheck(UUID addressId) {
        User currentUser = userService.getCurrentUser();
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);  // 403 Forbidden
        }
        return address;
    }

    private static Specification<UserAddress> hasKeyWord(String keyword) {
        return ((root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likeValue = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("province")), likeValue),
                    cb.like(cb.lower(root.get("district")), likeValue),
                    cb.like(cb.lower(root.get("ward")), likeValue),
                    cb.like(cb.lower(root.get("streetAddress")), likeValue),
                    cb.like(cb.lower(root.get("country")), likeValue));
        });
    }
}
