package spring.abtechzone.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.dto.ApiResponse;
import spring.abtechzone.dto.request.ProductRequest;
import spring.abtechzone.dto.response.ProductResponse;
import spring.abtechzone.service.ProductService;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {

    ProductService productService;

    @PostMapping
    ApiResponse<ProductResponse> createUser(@RequestBody @Valid ProductRequest request) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.create(request))
                .build();
    }

    @GetMapping("/{productId}")
    ApiResponse<ProductResponse> getUser(@PathVariable("productId") Long id) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProduct(id))
                .build();
    }

    @GetMapping
    ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponse.<List<ProductResponse>>builder()
                .result(productService.getProducts())
                .build();
    }

    @PutMapping("/{productId}")
    ApiResponse<ProductResponse> updateProduct(
            @PathVariable("productId") Long id, @RequestBody @Valid ProductRequest request) {
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
