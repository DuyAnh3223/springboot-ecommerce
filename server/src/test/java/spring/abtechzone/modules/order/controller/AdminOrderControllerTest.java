package spring.abtechzone.modules.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import spring.abtechzone.common.config.CustomJwtDecoder;
import spring.abtechzone.common.config.SecurityConfig;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.dto.response.OrderSummaryResponse;
import spring.abtechzone.modules.order.service.OrderLifecycleService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@WebMvcTest(AdminOrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"app.swagger.enabled=false"})
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderLifecycleService orderService;

    @MockitoBean
    private CustomJwtDecoder customJwtDecoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private void stubAdmin() {
        when(authService.getCurrentUsername()).thenReturn("admin-user");
        when(userRepository.findByUsername("admin-user"))
                .thenReturn(Optional.of(User.builder()
                        .id(ADMIN_ID)
                        .username("admin-user")
                        .isActive(true)
                        .build()));
    }

    @Test
    @DisplayName("Non-admin calling /admin/orders receives 403")
    @WithMockUser(username = "customer-user", authorities = "ROLE_USER")
    void nonAdmin_gets403() throws Exception {
        mockMvc.perform(get("/admin/orders").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin GET /admin/orders binds filters and returns the page")
    @WithMockUser(username = "admin-user", authorities = "ROLE_ADMIN")
    void adminSearch_bindsFilters() throws Exception {
        stubAdmin();
        when(orderService.getAdminOrders(any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(OrderSummaryResponse.builder()
                        .id(1L)
                        .orderCode("ORD-20260818-ABCD1234")
                        .status("PENDING")
                        .allowedTransitions(List.of("CANCELLED", "CONFIRMED"))
                        .build())));

        mockMvc.perform(get("/admin/orders")
                        .with(csrf())
                        .param("search", "ORD-2026")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].orderCode").value("ORD-20260818-ABCD1234"))
                .andExpect(jsonPath("$.result.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Admin PATCH /admin/orders/{orderCode}/status applies the shared service transition")
    @WithMockUser(username = "admin-user", authorities = "ROLE_ADMIN")
    void adminUpdateStatus_callsSharedPolicy() throws Exception {
        stubAdmin();
        when(orderService.updateOrderStatus(
                        eq("ORD-20260818-ABCD1234"), eq(OrderStatus.CONFIRMED), eq("Đã xác nhận"), any()))
                .thenReturn(OrderResponse.builder()
                        .id(1L)
                        .orderCode("ORD-20260818-ABCD1234")
                        .status("CONFIRMED")
                        .build());

        mockMvc.perform(patch("/admin/orders/ORD-20260818-ABCD1234/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "CONFIRMED", "note": "Đã xác nhận"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Admin status update with missing status returns 400")
    @WithMockUser(username = "admin-user", authorities = "ROLE_ADMIN")
    void adminUpdateStatus_missingStatus_returns400() throws Exception {
        stubAdmin();
        mockMvc.perform(patch("/admin/orders/ORD-20260818-ABCD1234/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"no status\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin invalid transition maps to 409")
    @WithMockUser(username = "admin-user", authorities = "ROLE_ADMIN")
    void adminUpdateStatus_invalidTransition_returns409() throws Exception {
        stubAdmin();
        when(orderService.updateOrderStatus(eq("ORD-X"), eq(OrderStatus.DELIVERED), any(), any()))
                .thenThrow(new AppException(ErrorCode.ORDER_STATUS_CONFLICT));

        mockMvc.perform(patch("/admin/orders/ORD-X/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_STATUS_CONFLICT.getCode()));
    }

    @Test
    @DisplayName("Admin order detail requires ADMIN and returns the snapshot")
    @WithMockUser(username = "admin-user", authorities = "ROLE_ADMIN")
    void adminDetail_requiresAdmin() throws Exception {
        stubAdmin();
        when(orderService.getAdminOrderDetail("ORD-20260818-ABCD1234"))
                .thenReturn(spring.abtechzone.modules.order.dto.response.OrderDetailResponse.builder()
                        .id(1L)
                        .orderCode("ORD-20260818-ABCD1234")
                        .status("CONFIRMED")
                        .build());

        mockMvc.perform(get("/admin/orders/ORD-20260818-ABCD1234").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.orderCode").value("ORD-20260818-ABCD1234"));
    }
}
