package spring.abtechzone.modules.inventory.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.inventory.dto.request.StockAdjustmentRequest;
import spring.abtechzone.modules.inventory.dto.request.StockMovementSearchRequest;
import spring.abtechzone.modules.inventory.dto.response.StockAdjustmentResponse;
import spring.abtechzone.modules.inventory.dto.response.StockMovementResponse;
import spring.abtechzone.modules.inventory.service.InventoryService;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin Inventory", description = "Single-warehouse stock adjustments and movement history")
public class AdminInventoryController {

    InventoryService inventoryService;

    @PostMapping("/{skuId}/adjustments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust stock", description = "Increase or decrease one SKU and append its audit movement")
    @ApiResponse(responseCode = "200", description = "Stock adjusted")
    @ApiResponse(responseCode = "400", description = "Invalid adjustment or insufficient stock")
    @ApiResponse(responseCode = "403", description = "Access denied")
    @ApiResponse(responseCode = "404", description = "Inventory not found")
    ApiResult<StockAdjustmentResponse> adjustStock(
            @PathVariable Long skuId, @RequestBody @Valid StockAdjustmentRequest request) {
        return ApiResult.<StockAdjustmentResponse>builder()
                .result(inventoryService.adjustStock(skuId, request))
                .build();
    }

    @GetMapping("/movements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stock movements", description = "Paged movement history ordered newest first")
    @ApiResponse(responseCode = "200", description = "Movement page returned")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Page<StockMovementResponse>> getStockMovements(
            @Valid @ModelAttribute StockMovementSearchRequest request) {
        return ApiResult.<Page<StockMovementResponse>>builder()
                .result(inventoryService.getStockMovements(request))
                .build();
    }
}
