package spring.abtechzone.modules.product.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.product.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.product.dto.request.SkuPreviewRequest;
import spring.abtechzone.modules.product.dto.response.ProductResponse;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.dto.response.SkuPreviewResponse;
import spring.abtechzone.modules.product.service.ProductService;
import spring.abtechzone.modules.product.service.ProductSkuService;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;
    ProductSkuService productSkuService;

    @GetMapping("/admin")
    ApiResponse<Page<ProductResponse>> getAdminProducts(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.getAdminProducts(request))
                .build();
    }

    @GetMapping("/admin/{productId}")
    ApiResponse<ProductResponse> getAdminProduct(@PathVariable("productId") Long id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build();
    }

    @PostMapping
    ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.create(request))
                .build();
    }

    @PatchMapping("/{productId}")
    ApiResponse<ProductResponse> updateProduct(
            @PathVariable("productId") Long id, @RequestBody @Valid ProductUpdateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.update(id, request))
                .build();
    }

    @PatchMapping("/{productId}/publish")
    ApiResponse<ProductResponse> publishProduct(@PathVariable("productId") Long productId) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.publishProduct(productId))
                .build();
    }

    @PostMapping("/{productId}/unpublish")
    ApiResponse<ProductResponse> unpublishProduct(@PathVariable("productId") Long productId) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.unpublishProduct(productId))
                .build();
    }

    @GetMapping("/{productId:\\d+}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("productId") Long id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build();
    }

    @GetMapping("/{slug:[a-zA-Z0-9\\-_]+}")
    ApiResponse<ProductResponse> getProductBySlug(@PathVariable("slug") String slug) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProductBySlug(slug))
                .build();
    }

    @GetMapping
    ApiResponse<Page<ProductResponse>> getProducts(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.getProducts(request))
                .build();
    }

    @DeleteMapping("/{productId}")
    ApiResponse<String> deleteProduct(@PathVariable("productId") Long id) {
        productService.delete(id);
        return ApiResponse.<String>builder().result("Product has been deleted").build();
    }

    @PostMapping("/{productId}/skus/preview")
    ApiResponse<List<SkuPreviewResponse>> previewSkus(
            @PathVariable("productId") Long productId, @RequestBody @Valid SkuPreviewRequest request) {
        return ApiResponse.<List<SkuPreviewResponse>>builder()
                .result(productSkuService.previewSkus(productId, request))
                .build();
    }

    @PostMapping("/{productId}/skus/bulk")
    ApiResponse<List<ProductSkuResponse>> createSkusBulk(
            @PathVariable("productId") Long productId, @RequestBody @Valid List<ProductSkuCreateRequest> requests) {
        return ApiResponse.<List<ProductSkuResponse>>builder()
                .result(productSkuService.createSkusBulk(productId, requests))
                .build();
    }
}
