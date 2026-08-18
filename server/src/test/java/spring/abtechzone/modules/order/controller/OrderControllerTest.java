package spring.abtechzone.modules.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import spring.abtechzone.common.config.CustomJwtDecoder;
import spring.abtechzone.common.config.SecurityConfig;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutItemResponse;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.VoucherReviewResponse;
import spring.abtechzone.modules.order.service.OrderService;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"app.swagger.enabled=false"})
@AutoConfigureMockMvc(addFilters = true)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CustomJwtDecoder customJwtDecoder;

    @Test
    @DisplayName("POST /orders/checkout-review - success returns reviewed snapshot")
    void checkoutReview_valid_returns200() throws Exception {
        CheckoutRequest request = CheckoutRequest.builder()
                .selectedSkuIds(List.of(17L))
                .voucherCode("SUMMER")
                .build();

        CheckoutResponse mockResponse = CheckoutResponse.builder()
                .items(List.of(CheckoutItemResponse.builder()
                        .skuId(17L)
                        .skuCode("SKU-17")
                        .productName("Tên sản phẩm")
                        .quantity(2)
                        .unitPrice(BigDecimal.valueOf(100000))
                        .lineTotal(BigDecimal.valueOf(200000))
                        .availableStock(5)
                        .build()))
                .subtotal(BigDecimal.valueOf(200000))
                .eligibleSubtotal(BigDecimal.valueOf(200000))
                .shippingFee(BigDecimal.valueOf(30000))
                .discountAmount(BigDecimal.valueOf(20000))
                .totalAmount(BigDecimal.valueOf(210000))
                .voucher(VoucherReviewResponse.builder()
                        .code("SUMMER")
                        .applicable(true)
                        .build())
                .canPlaceOrder(true)
                .build();

        when(orderService.checkoutReview(any(CheckoutRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/orders/checkout-review")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].skuId").value(17))
                .andExpect(jsonPath("$.result.items[0].lineTotal").value(200000))
                .andExpect(jsonPath("$.result.subtotal").value(200000))
                .andExpect(jsonPath("$.result.eligibleSubtotal").value(200000))
                .andExpect(jsonPath("$.result.shippingFee").value(30000))
                .andExpect(jsonPath("$.result.discountAmount").value(20000))
                .andExpect(jsonPath("$.result.totalAmount").value(210000))
                .andExpect(jsonPath("$.result.voucher.code").value("SUMMER"))
                .andExpect(jsonPath("$.result.voucher.applicable").value(true))
                .andExpect(jsonPath("$.result.canPlaceOrder").value(true))
                // Contract: no fingerprint/token/expiry
                .andExpect(jsonPath("$.result.reviewFingerprint").doesNotExist())
                .andExpect(jsonPath("$.result.reviewToken").doesNotExist())
                .andExpect(jsonPath("$.result.expiresAt").doesNotExist());
    }

    @Test
    @DisplayName("POST /orders/checkout-review - invalid request (missing selectedSkuIds) returns 400")
    void checkoutReview_missingSelection_returns400() throws Exception {
        mockMvc.perform(post("/orders/checkout-review")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voucherCode\":\"SUMMER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders/checkout-review - empty selection returns 400")
    void checkoutReview_emptySelection_returns400() throws Exception {
        mockMvc.perform(post("/orders/checkout-review")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedSkuIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders/checkout-review - non-positive SKU id returns 400")
    void checkoutReview_nonPositiveSkuId_returns400() throws Exception {
        mockMvc.perform(post("/orders/checkout-review")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedSkuIds\":[0]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders/checkout-review - expected business issue is HTTP 200 with canPlaceOrder=false")
    void checkoutReview_businessIssue_returns200Review() throws Exception {
        CheckoutRequest request =
                CheckoutRequest.builder().selectedSkuIds(List.of(17L)).build();

        CheckoutResponse mockResponse = CheckoutResponse.builder()
                .items(List.of(CheckoutItemResponse.builder()
                        .skuId(17L)
                        .quantity(2)
                        .issueCode(ErrorCode.INSUFFICIENT_STOCK.name())
                        .build()))
                .subtotal(BigDecimal.ZERO)
                .eligibleSubtotal(BigDecimal.ZERO)
                .shippingFee(BigDecimal.valueOf(30000))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(30000))
                .canPlaceOrder(false)
                .build();

        when(orderService.checkoutReview(any(CheckoutRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/orders/checkout-review")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].issueCode").value(ErrorCode.INSUFFICIENT_STOCK.name()))
                .andExpect(jsonPath("$.result.canPlaceOrder").value(false));
    }

    @Test
    @DisplayName("POST /orders/checkout-review - service exception maps to error code")
    void checkoutReview_ownerError_returnsError() throws Exception {
        CheckoutRequest request =
                CheckoutRequest.builder().selectedSkuIds(List.of(999L)).build();

        when(orderService.checkoutReview(any(CheckoutRequest.class)))
                .thenThrow(new AppException(ErrorCode.CART_ITEM_NOT_IN_CART));

        mockMvc.perform(post("/orders/checkout-review")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Auth contract evidence: the real SecurityConfig filter chain is active in this
     * class (addFilters=true), so an unauthenticated request is rejected by the
     * JwtAuthenticationEntryPoint with 401.
     */
    @Test
    @DisplayName("POST /orders/checkout-review - unauthenticated returns 401")
    void checkoutReview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/orders/checkout-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedSkuIds\":[1]}"))
                .andExpect(status().isUnauthorized());
    }
}
