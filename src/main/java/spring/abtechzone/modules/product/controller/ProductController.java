package spring.abtechzone.modules.product.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.product.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.product.dto.request.ProductSearchRequest;
import spring.abtechzone.modules.product.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.product.dto.response.ProductResponse;
import spring.abtechzone.modules.product.service.ProductService;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;

    @PostMapping
    ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductCreateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.create(request))
                .build();
    }

    @GetMapping("/{productId}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("productId") Long id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build();
    }

    @GetMapping
    ApiResponse<Page<ProductResponse>> getProducts(@Valid @ModelAttribute ProductSearchRequest request) {
        return ApiResponse.<Page<ProductResponse>>builder()
                .result(productService.getProducts(request))
                .build();
    }

    @PatchMapping("/{productId}")
    ApiResponse<ProductResponse> updateProduct(
            @PathVariable("productId") Long id, @RequestBody @Valid ProductUpdateRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.update(id, request))
                .build();
    }

    @DeleteMapping("/{productId}")
    ApiResponse<String> deleteProduct(@PathVariable("productId") Long id) {
        productService.delete(id);
        return ApiResponse.<String>builder().result("Product has been deleted").build();
    }
}
