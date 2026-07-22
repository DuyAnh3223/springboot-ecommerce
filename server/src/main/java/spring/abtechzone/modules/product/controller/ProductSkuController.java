package spring.abtechzone.modules.product.controller;

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
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.service.ProductSkuService;

@RestController
@RequestMapping("/skus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Product SKUs",
        description = "Standalone SKU management: search, create, update, and soft-delete individual product variants")
public class ProductSkuController {

    ProductSkuService productSkuService;

    @GetMapping
    @Operation(summary = "Get SKUs (paginated)", description = "Search and paginate product SKUs with optional filters")
    @ApiResponse(responseCode = "200", description = "SKUs retrieved")
    ApiResult<Page<ProductSkuResponse>> getSkus(@Valid @ModelAttribute ProductSkuSearchRequest request) {
        return ApiResult.<Page<ProductSkuResponse>>builder()
                .result(productSkuService.getSkus(request))
                .build();
    }

    @GetMapping("/{skuId}")
    @Operation(summary = "Get SKU by ID", description = "Retrieve a single product SKU by its ID")
    @ApiResponse(responseCode = "200", description = "SKU found")
    @ApiResponse(responseCode = "404", description = "SKU not found")
    ApiResult<ProductSkuResponse> getSku(@Parameter(description = "SKU ID") @PathVariable Long skuId) {
        return ApiResult.<ProductSkuResponse>builder()
                .result(productSkuService.getSku(skuId))
                .build();
    }

    @PostMapping
    @Operation(summary = "Create SKU", description = "Create a single SKU for an existing product")
    @ApiResponse(responseCode = "200", description = "SKU created")
    @ApiResponse(responseCode = "400", description = "Duplicate SKU or validation error")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<ProductSkuResponse> createSku(@RequestBody @Valid ProductSkuCreateRequest request) {
        return ApiResult.<ProductSkuResponse>builder()
                .result(productSkuService.createSku(request))
                .build();
    }

    @PatchMapping("/{skuId}")
    @Operation(summary = "Update SKU", description = "Update price, stock, or other fields of an existing SKU")
    @ApiResponse(responseCode = "200", description = "SKU updated")
    @ApiResponse(responseCode = "404", description = "SKU not found")
    @ApiResponse(responseCode = "400", description = "Validation error")
    ApiResult<ProductSkuResponse> updateSku(
            @Parameter(description = "SKU ID") @PathVariable Long skuId,
            @RequestBody @Valid ProductSkuUpdateRequest request) {
        return ApiResult.<ProductSkuResponse>builder()
                .result(productSkuService.updateSku(skuId, request))
                .build();
    }

    @DeleteMapping("/{skuId}")
    @Operation(summary = "Delete SKU", description = "Soft-delete a product SKU by ID")
    @ApiResponse(responseCode = "200", description = "SKU deleted")
    @ApiResponse(responseCode = "404", description = "SKU not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<String> deleteSku(@Parameter(description = "SKU ID") @PathVariable Long skuId) {
        productSkuService.deleteSku(skuId);
        return ApiResult.<String>builder()
                .result("Product SKU has been successfully deleted")
                .build();
    }
}
