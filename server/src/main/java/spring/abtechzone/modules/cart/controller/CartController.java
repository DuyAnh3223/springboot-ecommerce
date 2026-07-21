package spring.abtechzone.modules.cart.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.dto.ApiResult;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.UpdateQuantityRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.service.CartService;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
        name = "Cart",
        description =
                "Shopping cart management for the authenticated user: add, update, remove items and view current cart state")
public class CartController {

    CartService cartService;

    @PostMapping("/add")
    @Operation(
            summary = "Add item to cart",
            description =
                    "Add a SKU to the authenticated user's active cart. If the SKU is already in the cart, quantity is incremented. Stock availability is validated at add time")
    @ApiResponse(responseCode = "200", description = "Item added to cart")
    @ApiResponse(responseCode = "400", description = "Insufficient stock or invalid quantity")
    @ApiResponse(responseCode = "404", description = "SKU not found")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<CartResponse> addToCart(@RequestBody @Valid CartItemRequest request) {
        return ApiResult.<CartResponse>builder()
                .result(cartService.addToCart(request))
                .build();
    }

    @GetMapping
    @Operation(
            summary = "Get cart",
            description = "Retrieve the authenticated user's current active cart with all items")
    @ApiResponse(responseCode = "200", description = "Cart retrieved")
    @ApiResponse(responseCode = "404", description = "No active cart found")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<CartResponse> getCart() {
        return ApiResult.<CartResponse>builder().result(cartService.getCart()).build();
    }

    @DeleteMapping("/items/{skuId}")
    @Operation(summary = "Remove item from cart", description = "Remove a specific SKU from the cart by SKU ID")
    @ApiResponse(responseCode = "200", description = "Item removed from cart")
    @ApiResponse(responseCode = "404", description = "Cart item not found")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<Void> removeCartItem(@Parameter(description = "SKU ID to remove") @PathVariable Long skuId) {
        cartService.removeCartItem(skuId);
        return ApiResult.<Void>builder()
                .message("Cart item removed successfully")
                .build();
    }

    @PatchMapping("/items/{skuId}")
    @Operation(
            summary = "Update item quantity",
            description = "Update the quantity of a specific SKU in the cart. Stock availability is re-validated")
    @ApiResponse(responseCode = "200", description = "Quantity updated")
    @ApiResponse(responseCode = "400", description = "Insufficient stock or quantity < 1")
    @ApiResponse(responseCode = "404", description = "Cart item not found")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<CartItemResponse> updateCartItemQuantity(
            @Parameter(description = "SKU ID to update") @PathVariable Long skuId,
            @RequestBody @Valid UpdateQuantityRequest request) {
        return ApiResult.<CartItemResponse>builder()
                .result(cartService.updateCartItemQuantity(skuId, request))
                .build();
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Remove all items from the authenticated user's active cart")
    @ApiResponse(responseCode = "200", description = "Cart cleared")
    @ApiResponse(responseCode = "401", description = "Unauthenticated")
    ApiResult<Void> clearCart() {
        cartService.clearCart();
        return ApiResult.<Void>builder().message("Cart cleared successfully").build();
    }
}
