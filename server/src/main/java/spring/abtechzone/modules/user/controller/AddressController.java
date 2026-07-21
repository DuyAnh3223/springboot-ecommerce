package spring.abtechzone.modules.user.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.dto.ApiResult;
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
@Tag(name = "Addresses", description = "Manage shipping addresses for the authenticated user")
public class AddressController {

    AddressService addressService;

    @PostMapping
    @Operation(
            summary = "Create address",
            description =
                    "Add a new shipping address. If no default exists and isDefault=true, the new address becomes the default")
    @ApiResponse(responseCode = "200", description = "Address created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<AddressResponse> create(@RequestBody AddressRequest request) {
        return ApiResult.<AddressResponse>builder()
                .result(addressService.create(request))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get addresses (paginated)",
            description = "List the authenticated user's shipping addresses with optional search and pagination")
    @ApiResponse(responseCode = "200", description = "Addresses retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<Page<AddressResponse>> getAddresses(AddressSearchRequest request) {
        return ApiResult.<Page<AddressResponse>>builder()
                .result(addressService.getAddresses(request))
                .build();
    }

    @GetMapping("{addressId}")
    @Operation(
            summary = "Get address by ID",
            description = "Retrieve a single address. Returns ACCESS_DENIED if the address belongs to another user")
    @ApiResponse(responseCode = "200", description = "Address found")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @ApiResponse(responseCode = "403", description = "Address belongs to another user")
    ApiResult<AddressResponse> getAddress(@Parameter(description = "Address UUID") @PathVariable UUID addressId) {
        return ApiResult.<AddressResponse>builder()
                .result(addressService.getAddress(addressId))
                .build();
    }

    @PatchMapping("{addressId}")
    @Operation(summary = "Update address", description = "Update an existing address. Only the owner can update")
    @ApiResponse(responseCode = "200", description = "Address updated")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<AddressResponse> updateAddress(
            @Parameter(description = "Address UUID") @PathVariable UUID addressId,
            @RequestBody AddressRequest request) {
        return ApiResult.<AddressResponse>builder()
                .result(addressService.updateAddress(addressId, request))
                .build();
    }

    @DeleteMapping("{addressId}")
    @Operation(summary = "Delete address", description = "Delete a shipping address. Only the owner can delete")
    @ApiResponse(responseCode = "200", description = "Address deleted")
    @ApiResponse(responseCode = "404", description = "Address not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> deleteAddress(@Parameter(description = "Address UUID") @PathVariable UUID addressId) {
        addressService.deleteAddress(addressId);
        return ApiResult.<Void>builder().build();
    }

}
