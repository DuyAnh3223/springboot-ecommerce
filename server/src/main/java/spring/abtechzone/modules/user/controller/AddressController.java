package spring.abtechzone.modules.user.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.request.AddressSearchRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.service.AddressService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressController {
    AddressService addressService;

    @PostMapping
    ApiResponse<AddressResponse> create(@RequestBody AddressRequest request) {
        return ApiResponse.<AddressResponse>builder().result(addressService.create(request)).build();
    }

    @GetMapping
    ApiResponse<Page<AddressResponse>> getAddresses(AddressSearchRequest request) {
        return ApiResponse.<Page<AddressResponse>>builder().result(addressService.getAddresses(request)).build();
    }

    @GetMapping("{addressId}")
    ApiResponse<AddressResponse> getAddress(@PathVariable UUID addressId) {
        return ApiResponse.<AddressResponse>builder().result(addressService.getAddress(addressId)).build();
    }

    @PatchMapping("{addressId}")
    ApiResponse<AddressResponse> updateAddress(@PathVariable UUID addressId, @RequestBody AddressRequest request) {
        return ApiResponse.<AddressResponse>builder().result(addressService.updateAddress(addressId, request)).build();
    }

    @DeleteMapping("{addressId}")
    ApiResponse<Void> deleteAddress(@PathVariable UUID addressId) {
        addressService.deleteAddress(addressId);
        return ApiResponse.<Void>builder().build();
    }

}
