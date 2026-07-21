package spring.abtechzone.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.request.AddressSearchRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.mapper.AddressMapper;
import spring.abtechzone.modules.user.repository.UserAddressRepository;
import spring.abtechzone.modules.user.service.AddressService;
import spring.abtechzone.modules.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock
    UserAddressRepository userAddressRepository;

    @Mock
    AddressMapper addressMapper;

    @Mock
    UserService userService;

    @InjectMocks
    AddressService addressService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = new User();
        mockUser.setId(userId);
        when(userService.getCurrentUser()).thenReturn(mockUser);
    }

    @Test
    @DisplayName("Create address: when user has no default, new address becomes default")
    void create_whenNoExistingDefault_shouldSetDefaultTrue() {
        // Given
        AddressRequest request = new AddressRequest();
        request.setDefault(true);

        UserAddress address = new UserAddress();
        address.setDefault(true);

        when(addressMapper.toAddress(request)).thenReturn(address);
        when(userAddressRepository.existsByUserIdAndIsDefaultTrue(userId)).thenReturn(false);
        when(userAddressRepository.save(any())).thenReturn(address);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        // When
        AddressResponse response = addressService.create(request);

        // Then
        assertTrue(address.isDefault());
        verify(userAddressRepository).save(address);
    }

    @Test
    @DisplayName("Create address: when user already has default, new address should NOT be default")
    void create_whenExistingDefault_shouldSetDefaultFalse() {
        // Given
        AddressRequest request = new AddressRequest();
        request.setDefault(true);

        UserAddress address = new UserAddress();
        address.setDefault(true); // Request muốn default

        when(addressMapper.toAddress(request)).thenReturn(address);
        when(userAddressRepository.existsByUserIdAndIsDefaultTrue(userId)).thenReturn(true);
        when(userAddressRepository.save(any())).thenReturn(address);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        // When
        addressService.create(request);

        // Then
        assertFalse(address.isDefault()); // Bị ghi đè thành false
    }

    @Test
    @DisplayName("Create address: when request isDefault=false, keep as false")
    void create_whenRequestNotDefault_shouldKeepFalse() {
        AddressRequest request = new AddressRequest();
        request.setDefault(false);

        UserAddress address = new UserAddress();
        address.setDefault(false);

        when(addressMapper.toAddress(request)).thenReturn(address);
        // existsBy... không được gọi vì address.isDefault() = false
        when(userAddressRepository.save(any())).thenReturn(address);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        addressService.create(request);

        assertFalse(address.isDefault());
        verify(userAddressRepository, never()).existsByUserIdAndIsDefaultTrue(any());
    }

    // ========== GET ADDRESS ==========

    @Test
    @DisplayName("Get address: success when owner")
    void getAddress_whenOwner_shouldReturnAddress() {
        UUID addressId = UUID.randomUUID();
        UserAddress address = new UserAddress();
        address.setUser(mockUser);

        when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(addressMapper.toAddressResponse(address)).thenReturn(new AddressResponse());

        assertDoesNotThrow(() -> addressService.getAddress(addressId));
    }

    @Test
    @DisplayName("Get address: throw ACCESS_DENIED when not owner")
    void getAddress_whenNotOwner_shouldThrowAccessDenied() {
        UUID addressId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        UserAddress address = new UserAddress();
        address.setUser(otherUser);

        when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(address));

        AppException exception = assertThrows(AppException.class, () -> addressService.getAddress(addressId));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Get address: throw ADDRESS_NOT_FOUND when not exists")
    void getAddress_whenNotFound_shouldThrowNotFound() {
        UUID addressId = UUID.randomUUID();
        when(userAddressRepository.findById(addressId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> addressService.getAddress(addressId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, exception.getErrorCode());
    }

    // ========== UPDATE ==========

    @Test
    @DisplayName("Update address: success when owner")
    void updateAddress_whenOwner_shouldUpdate() {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest();

        UserAddress existing = new UserAddress();
        existing.setUser(mockUser);

        when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(existing));
        when(userAddressRepository.save(any())).thenReturn(existing);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        assertDoesNotThrow(() -> addressService.updateAddress(addressId, request));
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("Delete address: success when owner")
    void deleteAddress_whenOwner_shouldDelete() {
        UUID addressId = UUID.randomUUID();
        UserAddress existing = new UserAddress();
        existing.setUser(mockUser);

        when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(existing));

        assertDoesNotThrow(() -> addressService.deleteAddress(addressId));
        verify(userAddressRepository).delete(existing);
    }

    // ========== GET ADDRESSES (List) ==========

    @Test
    @DisplayName("Get addresses: should filter by current user only")
    void getAddresses_shouldFilterByUserId() {
        AddressSearchRequest request = new AddressSearchRequest();
        request.setSearch("Hanoi");
        Pageable pageable = PageRequest.of(0, 10);
        when(request.toPageable()).thenReturn(pageable); // Cần mock/spy

        Page<UserAddress> emptyPage = new PageImpl<>(Collections.emptyList());
        when(userAddressRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(emptyPage);

        addressService.getAddresses(request);

        verify(userAddressRepository).findAll(any(Specification.class), eq(pageable));
    }
}
