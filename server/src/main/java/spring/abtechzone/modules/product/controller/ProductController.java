package spring.abtechzone.modules.product.controller;

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
@Tag(
        name = "Products",
        description = "Product catalog management including CRUD, publish lifecycle, SKU preview and bulk creation")
public class ProductController {

    ProductService productService;
    ProductSkuService productSkuService;

    @GetMapping("/admin")
    @Operation(
            summary = "Get all products (Admin)",
            description = "Retrieve a paginated list of all products including drafts. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Products retrieved")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<Page<ProductResponse>> getAdminProducts(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResult.<Page<ProductResponse>>builder()
                .result(productService.getAdminProducts(request))
                .build();
    }

    @GetMapping("/admin/{productId}")
    @Operation(
            summary = "Get product by ID (Admin)",
            description = "Retrieve a single product by ID including draft state. Requires ADMIN role")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<ProductResponse> getAdminProduct(
            @Parameter(description = "Product ID") @PathVariable("productId") Long id) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build();
    }

    @PostMapping
    @Operation(
            summary = "Create product",
            description = "Create a new product in draft state. Category and at least one attribute are required")
    @ApiResponse(responseCode = "200", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Validation error or invalid attributes")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<ProductResponse> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.create(request))
                .build();
    }

    @PatchMapping("/{productId}")
    @Operation(
            summary = "Update product",
            description = "Update product fields. Category cannot be changed after creation")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<ProductResponse> updateProduct(
            @Parameter(description = "Product ID") @PathVariable("productId") Long id,
            @RequestBody @Valid ProductUpdateRequest request) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.update(id, request))
                .build();
    }

    @PatchMapping("/{productId}/publish")
    @Operation(
            summary = "Publish product",
            description =
                    "Transition a product from draft to published state. Product must have at least one active SKU")
    @ApiResponse(responseCode = "200", description = "Product published")
    @ApiResponse(responseCode = "400", description = "Product has no active SKUs")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<ProductResponse> publishProduct(@PathVariable @Parameter(description = "Product ID") Long productId) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.publishProduct(productId))
                .build();
    }

    @PostMapping("/{productId}/unpublish")
    @Operation(summary = "Unpublish product", description = "Revert a published product back to draft state")
    @ApiResponse(responseCode = "200", description = "Product unpublished")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<ProductResponse> unpublishProduct(@PathVariable @Parameter(description = "Product ID") Long productId) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.unpublishProduct(productId))
                .build();
    }

    @GetMapping("/{productId:\\d+}")
    @Operation(summary = "Get product by ID (Public)", description = "Retrieve a published product by numeric ID")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<ProductResponse> getProduct(@Parameter(description = "Product ID") @PathVariable("productId") Long id) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build();
    }

    @GetMapping("/{slug:[a-zA-Z0-9\\-_]+}")
    @Operation(
            summary = "Get product by slug (Public)",
            description = "Retrieve a published product by its URL-friendly slug")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<ProductResponse> getProductBySlug(
            @PathVariable @Parameter(description = "Product slug (e.g. iphone-16-pro)") String slug) {
        return ApiResult.<ProductResponse>builder()
                .result(productService.getProductBySlug(slug))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get products (paginated, Public)",
            description = "Retrieve a paginated list of published products with optional filtering and sorting")
    @ApiResponse(responseCode = "200", description = "Products retrieved")
    ApiResult<Page<ProductResponse>> getProducts(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResult.<Page<ProductResponse>>builder()
                .result(productService.getProducts(request))
                .build();
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete product", description = "Soft-delete a product and all its SKUs")
    @ApiResponse(responseCode = "200", description = "Product deleted")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "403", description = "Access denied")
    ApiResult<String> deleteProduct(@Parameter(description = "Product ID") @PathVariable("productId") Long id) {
        productService.delete(id);
        return ApiResult.<String>builder().result("Product has been deleted").build();
    }

    @PostMapping("/{productId}/skus/preview")
    @Operation(
            summary = "Preview SKU combinations",
            description = "Compute all possible SKU attribute combinations before committing bulk creation")
    @ApiResponse(responseCode = "200", description = "SKU previews generated")
    @ApiResponse(responseCode = "400", description = "Invalid attribute configuration")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<List<SkuPreviewResponse>> previewSkus(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @RequestBody @Valid SkuPreviewRequest request) {
        return ApiResult.<List<SkuPreviewResponse>>builder()
                .result(productSkuService.previewSkus(productId, request))
                .build();
    }

    @PostMapping("/{productId}/skus/bulk")
    @Operation(summary = "Bulk create SKUs", description = "Create multiple SKUs for a product in a single request")
    @ApiResponse(responseCode = "200", description = "SKUs created")
    @ApiResponse(responseCode = "400", description = "Duplicate SKU attributes or validation error")
    @ApiResponse(responseCode = "404", description = "Product not found")
    ApiResult<List<ProductSkuResponse>> createSkusBulk(
            @PathVariable @Parameter(description = "Product ID") Long productId,
            @RequestBody @Valid List<ProductSkuCreateRequest> requests) {
        return ApiResult.<List<ProductSkuResponse>>builder()
                .result(productSkuService.createSkusBulk(productId, requests))
                .build();
    }
}
