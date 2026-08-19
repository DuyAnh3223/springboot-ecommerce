package spring.abtechzone.modules.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import spring.abtechzone.common.exception.CheckoutChangedException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutItemRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedVoucherRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutItemResponse;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
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
                        .productName("TÃªn sáº£n pháº©m")
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

    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

    private ReviewedCheckoutRequest validReviewedCheckout() {
        return ReviewedCheckoutRequest.builder()
                .items(List.of(ReviewedCheckoutItemRequest.builder()
                        .skuId(17L)
                        .quantity(2)
                        .unitPrice(BigDecimal.valueOf(100000))
                        .lineTotal(BigDecimal.valueOf(200000))
                        .build()))
                .subtotal(BigDecimal.valueOf(200000))
                .eligibleSubtotal(BigDecimal.valueOf(200000))
                .shippingFee(BigDecimal.valueOf(30000))
                .discountAmount(BigDecimal.valueOf(20000))
                .totalAmount(BigDecimal.valueOf(210000))
                .voucher(ReviewedVoucherRequest.builder()
                        .code("SUMMER")
                        .applicable(true)
                        .build())
                .canPlaceOrder(true)
                .build();
    }

    @Test
    @DisplayName("POST /orders - success with Idempotency-Key returns created order")
    void createOrder_valid_returns200() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .reviewedCheckout(validReviewedCheckout())
                .addressId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class), eq(IDEMPOTENCY_KEY)))
                .thenReturn(OrderResponse.builder()
                        .id(999L)
                        .orderCode("ORD-20260818-ABCD1234")
                        .status("PENDING")
                        .subtotalAmount(BigDecimal.valueOf(200000))
                        .shippingFee(BigDecimal.valueOf(30000))
                        .discountAmount(BigDecimal.valueOf(20000))
                        .totalAmount(BigDecimal.valueOf(210000))
                        .build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(999))
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.totalAmount").value(210000));
    }

    @Test
    @DisplayName("POST /orders - missing Idempotency-Key returns 400")
    void createOrder_missingIdempotencyKey_returns400() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .reviewedCheckout(validReviewedCheckout())
                .addressId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .paymentMethod(PaymentMethod.COD)
                .build();

        mockMvc.perform(post("/orders")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders - non-UUID Idempotency-Key returns 400")
    void createOrder_invalidIdempotencyKey_returns400() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .reviewedCheckout(validReviewedCheckout())
                .addressId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class), anyString()))
                .thenThrow(new AppException(ErrorCode.INVALID_KEY));

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", "not-a-uuid")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders - invalid paymentMethod (not COD) fails binding with 400")
    void createOrder_nonCodPaymentMethod_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"reviewedCheckout": {
									"items": [{"skuId": 17, "quantity": 2, "unitPrice": 100000, "lineTotal": 200000}],
									"subtotal": 200000, "eligibleSubtotal": 200000, "shippingFee": 30000,
									"discountAmount": 20000, "totalAmount": 210000,
									"voucher": {"code": "SUMMER", "applicable": true}, "canPlaceOrder": true
								},
								"addressId": "33333333-3333-3333-3333-333333333333",
								"paymentMethod": "BANK_TRANSFER"
								}
								"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders - CHECKOUT_CHANGED 409 response contains the latest checkout in result")
    void createOrder_checkoutChanged_returns409WithLatestReview() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .reviewedCheckout(validReviewedCheckout())
                .addressId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .paymentMethod(PaymentMethod.COD)
                .build();

        CheckoutResponse latestReview = CheckoutResponse.builder()
                .items(List.of(CheckoutItemResponse.builder()
                        .skuId(17L)
                        .quantity(2)
                        .unitPrice(BigDecimal.valueOf(110000))
                        .lineTotal(BigDecimal.valueOf(220000))
                        .build()))
                .subtotal(BigDecimal.valueOf(220000))
                .eligibleSubtotal(BigDecimal.valueOf(220000))
                .shippingFee(BigDecimal.valueOf(30000))
                .discountAmount(BigDecimal.valueOf(20000))
                .totalAmount(BigDecimal.valueOf(230000))
                .canPlaceOrder(true)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class), eq(IDEMPOTENCY_KEY)))
                .thenThrow(new CheckoutChangedException(latestReview));

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CHECKOUT_CHANGED.getCode()))
                .andExpect(jsonPath("$.result.items[0].unitPrice").value(110000))
                .andExpect(jsonPath("$.result.totalAmount").value(230000));
    }

    @Test
    @DisplayName("POST /orders - duplicate reviewed SKU IDs return 400")
    void createOrder_duplicateSkuIds_returns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"reviewedCheckout": {
									"items": [
										{"skuId": 17, "quantity": 2, "unitPrice": 100000, "lineTotal": 200000},
										{"skuId": 17, "quantity": 1, "unitPrice": 100000, "lineTotal": 100000}
									],
									"subtotal": 300000, "eligibleSubtotal": 300000, "shippingFee": 30000,
									"discountAmount": 0, "totalAmount": 330000, "canPlaceOrder": true
								},
								"addressId": "33333333-3333-3333-3333-333333333333",
								"paymentMethod": "COD"
								}
								"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /orders - IDEMPOTENCY_KEY_REUSED maps to 409")
    void createOrder_idempotencyReused_returns409() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .reviewedCheckout(validReviewedCheckout())
                .addressId(java.util.UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .paymentMethod(PaymentMethod.COD)
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class), eq(IDEMPOTENCY_KEY)))
                .thenThrow(new AppException(ErrorCode.IDEMPOTENCY_KEY_REUSED));

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.IDEMPOTENCY_KEY_REUSED.getCode()));
    }
}
