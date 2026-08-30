package spring.abtechzone.modules.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import spring.abtechzone.modules.cart.constant.CartMergeItemStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.UpdateQuantityRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartMergeItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartMergeResponse;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.service.CartService;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CartService cartService;

    @Test
    @DisplayName("POST /cart/add - success returns HTTP 200 and CartResponse")
    void addToCart_valid_returns200() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductSkuId(10L);
        request.setQuantity(2);

        CartResponse mockResponse = CartResponse.builder()
                .cartId(1L)
                .userId("test-user")
                .items(List.of())
                .build();

        when(cartService.addToCart(any(CartItemRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.cartId").value(1));
    }

    @Test
    @DisplayName("POST /cart/merge - returns per-item merge result")
    void mergeGuestCart_valid_returnsBatchResult() throws Exception {
        UUID mergeId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        CartMergeResponse mergeResponse = CartMergeResponse.builder()
                .mergeId(mergeId)
                .items(List.of(CartMergeItemResponse.builder()
                        .skuId(17L)
                        .requestedQuantity(2)
                        .mergedQuantity(2)
                        .status(CartMergeItemStatus.MERGED)
                        .build()))
                .build();
        when(cartService.mergeGuestCart(any())).thenReturn(mergeResponse);

        mockMvc.perform(post("/cart/merge")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"mergeId":"550e8400-e29b-41d4-a716-446655440000","items":[{"skuId":17,"quantity":2}]}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.mergeId").value(mergeId.toString()))
                .andExpect(jsonPath("$.result.items[0].status").value("MERGED"));
    }

    @Test
    @DisplayName("POST /cart/add - service exception returns error code")
    void addToCart_notFound_returnsError() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductSkuId(999L);
        request.setQuantity(2);

        when(cartService.addToCart(any(CartItemRequest.class)))
                .thenThrow(new spring.abtechzone.common.exception.AppException(
                        spring.abtechzone.common.exception.ErrorCode.SKU_NOT_FOUND));

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /cart/add - invalid request format (null productSkuId) returns 400 Bad Request")
    void addToCart_nullProductSkuId_returns400() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductSkuId(null);
        request.setQuantity(2);

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cart/add - invalid request format (null quantity) returns 400 Bad Request")
    void addToCart_nullQuantity_returns400() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductSkuId(10L);
        request.setQuantity(null);

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cart/add - invalid request format (zero quantity) returns 400 Bad Request")
    void addToCart_zeroQuantity_returns400() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductSkuId(10L);
        request.setQuantity(0);

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /cart/add - invalid request format (negative quantity) returns 400 Bad Request")
    void addToCart_negativeQuantity_returns400() throws Exception {
        CartItemRequest request = new CartItemRequest();
        request.setProductSkuId(10L);
        request.setQuantity(-5);

        mockMvc.perform(post("/cart/add")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /cart - returns active cart")
    void getCart_success_returnsCart() throws Exception {
        CartResponse mockResponse = CartResponse.builder()
                .cartId(1L)
                .userId("test-user")
                .items(List.of())
                .build();

        when(cartService.getCart()).thenReturn(mockResponse);

        mockMvc.perform(get("/cart").with(jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.cartId").value(1));
    }

    @Test
    @DisplayName("DELETE /cart/items/{skuId} - removes item and returns success message")
    void removeCartItem_success_returnsMessage() throws Exception {
        mockMvc.perform(delete("/cart/items/10").with(csrf()).with(jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item removed successfully"));

        verify(cartService).removeCartItem(10L);
    }

    @Test
    @DisplayName("PATCH /cart/items/{skuId} - updates quantity and returns CartItemResponse")
    void updateCartItemQuantity_success_returnsUpdatedItem() throws Exception {
        UpdateQuantityRequest request = new UpdateQuantityRequest();
        request.setQuantity(5);

        CartItemResponse mockResponse = CartItemResponse.builder()
                .productSkuId(10L)
                .quantity(5)
                .unitPrice(new BigDecimal("99.99"))
                .build();

        when(cartService.updateCartItemQuantity(eq(10L), any(UpdateQuantityRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(patch("/cart/items/10")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.quantity").value(5));
    }

    @Test
    @DisplayName("DELETE /cart - clears cart and returns message")
    void clearCart_success_returnsMessage() throws Exception {
        mockMvc.perform(delete("/cart").with(csrf()).with(jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));

        verify(cartService).clearCart();
    }
}
