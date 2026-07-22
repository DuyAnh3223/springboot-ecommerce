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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.request.AddressSearchRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.entity.Address;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.mapper.AddressMapper;
import spring.abtechzone.modules.user.repository.AddressRepository;
import spring.abtechzone.modules.user.service.AddressService;
import spring.abtechzone.modules.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

    @Mock
    AddressRepository addressRepository;

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
    @DisplayName("Create address: when request isDefault=true, should unset existing defaults and keep isDefault=true")
    void create_whenRequestDefaultTrue_shouldUnsetExistingAndKeepDefaultTrue() {
        // Given
        AddressRequest request = new AddressRequest();
        request.setIsDefault(true);

        Address address = new Address();
        address.setIsDefault(true);

        when(addressMapper.toAddress(request)).thenReturn(address);
        when(addressRepository.existsByUserIdAndIsDefaultTrue(userId)).thenReturn(true);
        when(addressRepository.save(any())).thenReturn(address);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        // When
        AddressResponse response = addressService.create(request);

        // Then
        assertTrue(address.getIsDefault());
        verify(addressRepository).unsetDefaultAddressesByUserId(userId);
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName(
            "Create address: when user has no existing default and request isDefault=false, automatically set isDefault=true")
    void create_whenNoExistingDefaultAndRequestDefaultFalse_shouldSetDefaultTrue() {
        // Given
        AddressRequest request = new AddressRequest();
        request.setIsDefault(false);

        Address address = new Address();
        address.setIsDefault(false);

        when(addressMapper.toAddress(request)).thenReturn(address);
        when(addressRepository.existsByUserIdAndIsDefaultTrue(userId)).thenReturn(false);
        when(addressRepository.save(any())).thenReturn(address);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        // When
        addressService.create(request);

        // Then
        assertTrue(address.getIsDefault());
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("Create address: when user has existing default and request isDefault=false, keep as false")
    void create_whenExistingDefaultAndRequestDefaultFalse_shouldKeepFalse() {
        // Given
        AddressRequest request = new AddressRequest();
        request.setIsDefault(false);

        Address address = new Address();
        address.setIsDefault(false);

        when(addressMapper.toAddress(request)).thenReturn(address);
        when(addressRepository.existsByUserIdAndIsDefaultTrue(userId)).thenReturn(true);
        when(addressRepository.save(any())).thenReturn(address);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        // When
        addressService.create(request);

        // Then
        assertFalse(address.getIsDefault());
        verify(addressRepository, never()).unsetDefaultAddressesByUserId(any());
        verify(addressRepository).save(address);
    }

    // ========== GET ADDRESS ==========

    @Test
    @DisplayName("Get address: success when owner")
    void getAddress_whenOwner_shouldReturnAddress() {
        UUID addressId = UUID.randomUUID();
        Address address = new Address();
        address.setUser(mockUser);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(addressMapper.toAddressResponse(address)).thenReturn(new AddressResponse());

        assertDoesNotThrow(() -> addressService.getAddress(addressId));
    }

    @Test
    @DisplayName("Get address: throw ACCESS_DENIED when not owner")
    void getAddress_whenNotOwner_shouldThrowAccessDenied() {
        UUID addressId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        Address address = new Address();
        address.setUser(otherUser);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        AppException exception = assertThrows(AppException.class, () -> addressService.getAddress(addressId));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Get address: throw ADDRESS_NOT_FOUND when not exists")
    void getAddress_whenNotFound_shouldThrowNotFound() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findById(addressId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> addressService.getAddress(addressId));

        assertEquals(ErrorCode.ADDRESS_NOT_FOUND, exception.getErrorCode());
    }

    // ========== UPDATE ==========

    @Test
    @DisplayName("Update address: success when owner")
    void updateAddress_whenOwner_shouldUpdate() {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest();

        Address existing = new Address();
        existing.setUser(mockUser);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any())).thenReturn(existing);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        assertDoesNotThrow(() -> addressService.updateAddress(addressId, request));
    }

    @Test
    @DisplayName("Update address: when owner and request isDefault=true, should unset existing defaults")
    void updateAddress_whenRequestDefaultTrue_shouldUnsetExistingDefault() {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest();
        request.setIsDefault(true);

        Address existing = new Address();
        existing.setUser(mockUser);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existing));
        when(addressRepository.save(any())).thenReturn(existing);
        when(addressMapper.toAddressResponse(any())).thenReturn(new AddressResponse());

        addressService.updateAddress(addressId, request);

        verify(addressRepository).unsetDefaultAddressesByUserId(userId);
        verify(addressRepository).save(existing);
    }

    // ========== DELETE ==========

    @Test
    @DisplayName("Delete address: success when owner")
    void deleteAddress_whenOwner_shouldDelete() {
        UUID addressId = UUID.randomUUID();
        Address existing = new Address();
        existing.setUser(mockUser);
        existing.setIsDefault(false);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existing));

        assertDoesNotThrow(() -> addressService.deleteAddress(addressId));
        verify(addressRepository).delete(existing);
    }

    @Test
    @DisplayName(
            "Delete address: when deleting default address and other addresses exist, set first remaining as default")
    void deleteAddress_whenDefaultAddress_shouldPromoteNextAddressToDefault() {
        UUID addressId = UUID.randomUUID();
        Address existing = new Address();
        existing.setUser(mockUser);
        existing.setIsDefault(true);

        Address remainingAddress = new Address();
        remainingAddress.setUser(mockUser);
        remainingAddress.setIsDefault(false);

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(existing));
        when(addressRepository.findByUserId(userId)).thenReturn(java.util.List.of(remainingAddress));

        addressService.deleteAddress(addressId);

        verify(addressRepository).delete(existing);
        assertTrue(remainingAddress.getIsDefault());
        verify(addressRepository).save(remainingAddress);
    }

    // ========== GET ADDRESSES (List) ==========

    @Test
    @DisplayName("Get addresses: should filter by current user only")
    void getAddresses_shouldFilterByUserId() {
        AddressSearchRequest request = new AddressSearchRequest();
        request.setSearch("Hanoi");
        request.setPage(1);
        request.setSize(10);
        Pageable pageable = request.toPageable();

        Page<Address> emptyPage = new PageImpl<>(Collections.emptyList());
        when(addressRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        addressService.getAddresses(request);

        verify(addressRepository).findAll(any(Specification.class), eq(pageable));
    }
}
