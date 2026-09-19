package spring.abtechzone.modules.shipment.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.dto.ShippingDistrictResponse;
import spring.abtechzone.modules.shipment.dto.ShippingProvinceResponse;
import spring.abtechzone.modules.shipment.dto.ShippingWardResponse;
import spring.abtechzone.modules.shipment.service.ShippingLocationService;

@RestController
@RequestMapping("/shipping/locations")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Shipping locations", description = "GHN province, district and ward catalogue")
public class ShippingLocationController {
    ShippingLocationService service;

    @GetMapping("/provinces")
    @Operation(summary = "Get shipping provinces")
    @ApiResponse(responseCode = "200", description = "Province list")
    @ApiResponse(responseCode = "503", description = "GHN location provider unavailable")
    ApiResult<List<ShippingProvinceResponse>> getProvinces() {
        return ApiResult.<List<ShippingProvinceResponse>>builder()
                .result(service.getProvinces())
                .build();
    }

    @GetMapping("/districts")
    @Operation(summary = "Get shipping districts")
    @ApiResponse(responseCode = "200", description = "District list")
    @ApiResponse(responseCode = "400", description = "Invalid province ID")
    ApiResult<List<ShippingDistrictResponse>> getDistricts(@RequestParam Integer provinceId) {
        if (provinceId == null || provinceId <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return ApiResult.<List<ShippingDistrictResponse>>builder()
                .result(service.getDistricts(provinceId))
                .build();
    }

    @GetMapping("/wards")
    @Operation(summary = "Get shipping wards")
    @ApiResponse(responseCode = "200", description = "Ward list")
    @ApiResponse(responseCode = "400", description = "Invalid district ID")
    ApiResult<List<ShippingWardResponse>> getWards(@RequestParam Integer districtId) {
        if (districtId == null || districtId <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        return ApiResult.<List<ShippingWardResponse>>builder()
                .result(service.getWards(districtId))
                .build();
    }
}
