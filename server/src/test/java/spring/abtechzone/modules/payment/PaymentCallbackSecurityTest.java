package spring.abtechzone.modules.payment;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import spring.abtechzone.common.config.*;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.payment.config.OnlinePaymentProperties;
import spring.abtechzone.modules.payment.controller.*;
import spring.abtechzone.modules.payment.service.*;
import spring.abtechzone.modules.user.repository.UserRepository;

@WebMvcTest({PaymentCallbackController.class, PaymentCheckoutController.class, AdminPaymentController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"jwt.signerKey=test-test-test-test-test-test-test-test", "app.swagger.enabled=false"})
class PaymentCallbackSecurityTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    PaymentCallbackService callbacks;

    @MockitoBean
    OnlinePaymentService online;

    @MockitoBean
    OnlinePaymentProperties properties;

    @MockitoBean
    OrderPaymentService reads;

    @MockitoBean
    AuthService auth;

    @MockitoBean
    UserRepository users;

    @MockitoBean
    CustomJwtDecoder decoder;

    @Test
    void anonymousMomoCallbackReachesVerifierThroughRealSecurityFilters() throws Exception {
        doReturn(ResponseEntity.noContent().build()).when(callbacks).json(any(), any());
        mvc.perform(post("/payments/callbacks/momo")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNoContent());
        verify(callbacks).json(any(), any());
    }

    @Test
    void anonymousCheckoutCannotInitiatePayment() throws Exception {
        mvc.perform(post("/orders/ORD1/payments/checkout")
                        .contentType("application/json")
                        .content("{\"provider\":\"MOMO\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(online);
    }

    @Test
    void customerCannotReadAdminAttempts() throws Exception {
        when(decoder.decode("customer-token"))
                .thenReturn(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("customer-token")
                        .header("alg", "HS512")
                        .subject("customer")
                        .claim("scope", "ROLE_USER")
                        .build());
        mvc.perform(get("/admin/orders/ORD1/payments").header("Authorization", "Bearer customer-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reads);
    }

    @Test
    void unknownCallbackRouteIsNotPublic() throws Exception {
        mvc.perform(post("/payments/callbacks/unknown")).andExpect(status().isUnauthorized());
        verifyNoInteractions(callbacks);
    }
}
