package spring.abtechzone.modules.cart.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResponse;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.UpdateQuantityRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.service.CartService;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartController {

    CartService cartService;

    @PostMapping("/add")
    ApiResponse<CartResponse> addToCart(@RequestBody @Valid CartItemRequest request) {
        return ApiResponse.<CartResponse>builder()
                .result(cartService.addToCart(request))
                .build();
    }

    @GetMapping
    ApiResponse<CartResponse> getCart() {
        return ApiResponse.<CartResponse>builder().result(cartService.getCart()).build();
    }

    @DeleteMapping("/items/{skuId}")
    ApiResponse<Void> removeCartItem(@PathVariable Long skuId) {
        cartService.removeCartItem(skuId);
        return ApiResponse.<Void>builder()
                .message("Cart item removed successfully")
                .build();
    }

    @PatchMapping("/items/{skuId}")
    ApiResponse<CartItemResponse> updateCartItemQuantity(
            @PathVariable Long skuId, @RequestBody @Valid UpdateQuantityRequest request) {
        return ApiResponse.<CartItemResponse>builder()
                .result(cartService.updateCartItemQuantity(skuId, request))
                .build();
    }

    @DeleteMapping
    ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.<Void>builder().message("Cart cleared successfully").build();
    }
}
