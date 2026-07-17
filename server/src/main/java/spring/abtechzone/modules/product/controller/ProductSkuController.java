package spring.abtechzone.modules.product.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.service.ProductSkuService;

@RestController
@RequestMapping("/skus")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuController {

    ProductSkuService productSkuService;

    @GetMapping
    public ApiResponse<Page<ProductSkuResponse>> getSkus(@Valid @ModelAttribute ProductSkuSearchRequest request) {
        return ApiResponse.<Page<ProductSkuResponse>>builder()
                .result(productSkuService.getSkus(request))
                .build();
    }

    @GetMapping("/{skuId}")
    public ApiResponse<ProductSkuResponse> getSku(@PathVariable Long skuId) {
        return ApiResponse.<ProductSkuResponse>builder()
                .result(productSkuService.getSku(skuId))
                .build();
    }

    @PostMapping
    public ApiResponse<ProductSkuResponse> createSku(@RequestBody @Valid ProductSkuCreateRequest request) {
        return ApiResponse.<ProductSkuResponse>builder()
                .result(productSkuService.createSku(request))
                .build();
    }

    @PatchMapping("/{skuId}")
    public ApiResponse<ProductSkuResponse> updateSku(
            @PathVariable Long skuId, @RequestBody @Valid ProductSkuUpdateRequest request) {
        return ApiResponse.<ProductSkuResponse>builder()
                .result(productSkuService.updateSku(skuId, request))
                .build();
    }

    @DeleteMapping("/{skuId}")
    public ApiResponse<String> deleteSku(@PathVariable Long skuId) {
        productSkuService.deleteSku(skuId);
        return ApiResponse.<String>builder()
                .result("Product SKU has been successfully deleted")
                .build();
    }
}
