package spring.abtechzone.modules.voucher.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
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
public class VoucherController {
    VoucherService voucherService;

    @PostMapping
    ApiResponse<VoucherResponse> createVoucher(@RequestBody @Valid VoucherCreateRequest voucherCreateRequest) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.create(voucherCreateRequest))
                .build();
    }

    @GetMapping
    ApiResponse<Page<VoucherResponse>> getVouchers(@Valid @ModelAttribute VoucherSearchRequest request) {
        return ApiResponse.<Page<VoucherResponse>>builder()
                .result(voucherService.getVouchers(request))
                .build();
    }

    @GetMapping("/{code}")
    ApiResponse<VoucherResponse> getVoucher(@PathVariable String code) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.getVoucher(code))
                .build();
    }

    @PatchMapping("/{code}")
    ApiResponse<VoucherResponse> updateVoucher(
            @PathVariable String code, @RequestBody @Valid VoucherUpdateRequest voucherUpdateRequest) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.update(code, voucherUpdateRequest))
                .build();
    }

    @DeleteMapping("/{code}")
    ApiResponse<Void> deleteVoucher(@PathVariable String code) {
        voucherService.delete(code);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/{code}/products")
    ApiResponse<List<ProductSkuResponse>> getProductSkusByVoucherCode(@PathVariable String code) {
        return ApiResponse.<List<ProductSkuResponse>>builder()
                .result(voucherService.getAllProductSkusByVoucherCode(code))
                .build();
    }

    @PostMapping("/validate")
    ApiResponse<VoucherDiscountResponse> validateVoucher(@RequestBody @Valid VoucherDiscountRequest request) {
        return ApiResponse.<VoucherDiscountResponse>builder()
                .result(voucherService.calculateDiscount(request))
                .build();
    }
}
