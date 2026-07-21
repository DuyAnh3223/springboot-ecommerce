package spring.abtechzone.modules.voucher.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherDiscountRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherSearchRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
import spring.abtechzone.modules.voucher.dto.response.VoucherDiscountResponse;
import spring.abtechzone.modules.voucher.dto.response.VoucherResponse;
import spring.abtechzone.modules.voucher.service.VoucherService;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Vouchers",
        description =
                "Voucher / discount coupon management. Supports FIXED_AMOUNT and PERCENTAGE discount types with per-user limits, expiry, and min-order-value constraints")
public class VoucherController {

    VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create voucher", description = "Create a new voucher. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Voucher created")
    @ApiResponse(responseCode = "400", description = "Voucher code already exists or validation error")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<VoucherResponse> createVoucher(@RequestBody @Valid VoucherCreateRequest voucherCreateRequest) {
        return ApiResult.<VoucherResponse>builder()
                .result(voucherService.create(voucherCreateRequest))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get vouchers (paginated)",
            description = "Retrieve a paginated list of vouchers with optional search. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Vouchers retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Page<VoucherResponse>> getVouchers(@Valid @ModelAttribute VoucherSearchRequest request) {
        return ApiResult.<Page<VoucherResponse>>builder()
                .result(voucherService.getVouchers(request))
                .build();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get voucher by code", description = "Retrieve a single voucher by its unique code")
    @ApiResponse(responseCode = "200", description = "Voucher found")
    @ApiResponse(responseCode = "404", description = "Voucher not found")
    ApiResult<VoucherResponse> getVoucher(
            @Parameter(description = "Voucher code (e.g. SUMMER25)") @PathVariable String code) {
        return ApiResult.<VoucherResponse>builder()
                .result(voucherService.getVoucher(code))
                .build();
    }

    @PatchMapping("/{code}")
    @Operation(summary = "Update voucher", description = "Update a voucher's configuration. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Voucher updated")
    @ApiResponse(responseCode = "404", description = "Voucher not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<VoucherResponse> updateVoucher(
            @Parameter(description = "Voucher code") @PathVariable String code,
            @RequestBody @Valid VoucherUpdateRequest voucherUpdateRequest) {
        return ApiResult.<VoucherResponse>builder()
                .result(voucherService.update(code, voucherUpdateRequest))
                .build();
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete voucher", description = "Delete a voucher by code. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Voucher deleted")
    @ApiResponse(responseCode = "404", description = "Voucher not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Void> deleteVoucher(@Parameter(description = "Voucher code") @PathVariable String code) {
        voucherService.delete(code);
        return ApiResult.<Void>builder().build();
    }

    @GetMapping("/{code}/products")
    @Operation(
            summary = "Get applicable SKUs",
            description =
                    "List all product SKUs that this voucher can be applied to (only relevant when applyScope = SPECIFIC_SKUS)")
    @ApiResponse(responseCode = "200", description = "Applicable SKUs retrieved")
    @ApiResponse(responseCode = "404", description = "Voucher not found")
    ApiResult<List<ProductSkuResponse>> getProductSkusByVoucherCode(
            @Parameter(description = "Voucher code") @PathVariable String code) {
        return ApiResult.<List<ProductSkuResponse>>builder()
                .result(voucherService.getAllProductSkusByVoucherCode(code))
                .build();
    }

    @PostMapping("/validate")
    @Operation(
            summary = "Validate and calculate discount",
            description =
                    "Validate a voucher code against a given order subtotal and return the computed discount amount")
    @ApiResponse(responseCode = "200", description = "Voucher valid, discount calculated")
    @ApiResponse(responseCode = "400", description = "Voucher expired, exhausted, or below minimum order value")
    @ApiResponse(responseCode = "404", description = "Voucher not found")
    ApiResult<VoucherDiscountResponse> validateVoucher(@RequestBody @Valid VoucherDiscountRequest request) {
        return ApiResult.<VoucherDiscountResponse>builder()
                .result(voucherService.calculateDiscount(request))
                .build();
    }
}
