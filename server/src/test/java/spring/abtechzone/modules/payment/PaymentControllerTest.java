package spring.abtechzone.modules.payment;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import spring.abtechzone.common.config.CustomJwtDecoder;
import spring.abtechzone.common.config.SecurityConfig;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.payment.controller.AdminPaymentController;
import spring.abtechzone.modules.payment.controller.PaymentController;
import spring.abtechzone.modules.payment.controller.PaymentMockController;
import spring.abtechzone.modules.payment.dto.response.*;
import spring.abtechzone.modules.payment.service.OrderPaymentService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@WebMvcTest({PaymentController.class, AdminPaymentController.class, PaymentMockController.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.swagger.enabled=false", "app.payment.mock-enabled=true"})
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderPaymentService orderPaymentService;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    UserRepository userRepository;

    @MockitoBean
    CustomJwtDecoder customJwtDecoder;

    @Test
    void ownerRead_returnsSummaryWithoutRawPayload() throws Exception {
        User user = stubUser();
        when(orderPaymentService.getMyPayments("ORD-1", user)).thenReturn(overview());

        mockMvc.perform(get("/orders/ORD-1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.result.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.result.attempts[0].provider").value("INTERNAL"))
                .andExpect(jsonPath("$.result.attempts[0].rawPayload").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminMockResult_bindsServerValidatedPayload() throws Exception {
        User admin = stubUser();
        when(orderPaymentService.applyMockResult(eq(20L), any(), eq(admin))).thenReturn(command());

        mockMvc.perform(post("/payments/20/mock-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"providerReference": "TX-1",
								"outcome": "SUCCEEDED",
								"amount": 100000,
								"currency": "VND"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.result.paymentStatus").value("PAID"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void nonAdminMockResult_returns403() throws Exception {
        mockMvc.perform(post("/payments/20/mock-result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"providerReference": "TX-1",
								"outcome": "SUCCEEDED",
								"amount": 100000,
								"currency": "VND"
								}
								"""))
                .andExpect(status().isForbidden());
    }

    private User stubUser() {
        User user = User.builder().id(UUID.randomUUID()).username("user").build();
        when(authService.getCurrentUsername()).thenReturn("user");
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        return user;
    }

    private OrderPaymentsResponse overview() {
        return OrderPaymentsResponse.builder()
                .orderCode("ORD-1")
                .orderStatus("PENDING")
                .paymentMethod("COD")
                .paymentStatus("UNPAID")
                .attempts(List.of(PaymentAttemptResponse.builder()
                        .id(20L)
                        .provider("INTERNAL")
                        .method("COD")
                        .status("PENDING")
                        .amount(new BigDecimal("100000.00"))
                        .currency("VND")
                        .build()))
                .build();
    }

    private PaymentCommandResponse command() {
        return PaymentCommandResponse.builder()
                .orderCode("ORD-1")
                .orderStatus("CONFIRMED")
                .paymentStatus("PAID")
                .payment(PaymentAttemptResponse.builder()
                        .id(20L)
                        .status("SUCCEEDED")
                        .build())
                .build();
    }
}
