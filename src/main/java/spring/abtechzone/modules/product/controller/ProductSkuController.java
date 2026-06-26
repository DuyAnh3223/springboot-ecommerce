package spring.abtechzone.modules.product.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.service.ProductSkuService;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductSkuController {

    ProductSkuService productSkuService;

    @GetMapping("/skus/{skuId}")
    public ApiResponse<ProductSkuResponse> getSku(@PathVariable Long skuId) {
        return ApiResponse.<ProductSkuResponse>builder()
                .result(productSkuService.getSku(skuId))
                .build();
    }

    @PostMapping("/{productId}/skus")
    public ApiResponse<ProductSkuResponse> createSku(
            @PathVariable Long productId, @RequestBody @Valid ProductSkuCreateRequest request) {
        return ApiResponse.<ProductSkuResponse>builder()
                .result(productSkuService.createSku(productId, request))
                .build();
    }

    @PatchMapping("/skus/{skuId}")
    public ApiResponse<ProductSkuResponse> updateSku(
            @PathVariable Long skuId, @RequestBody @Valid ProductSkuUpdateRequest request) {
        return ApiResponse.<ProductSkuResponse>builder()
                .result(productSkuService.updateSku(skuId, request))
                .build();
    }

    @DeleteMapping("/skus/{skuId}")
    public ApiResponse<String> deleteSku(@PathVariable Long skuId) {
        productSkuService.deleteSku(skuId);
        return ApiResponse.<String>builder()
                .result("Product SKU has been successfully deleted")
                .build();
    }
}
