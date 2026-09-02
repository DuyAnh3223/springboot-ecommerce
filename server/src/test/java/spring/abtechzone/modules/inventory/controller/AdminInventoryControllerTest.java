package spring.abtechzone.modules.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import spring.abtechzone.common.config.CustomJwtDecoder;
import spring.abtechzone.common.config.SecurityConfig;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;
import spring.abtechzone.modules.inventory.dto.request.StockAdjustmentRequest;
import spring.abtechzone.modules.inventory.dto.request.StockMovementSearchRequest;
import spring.abtechzone.modules.inventory.dto.response.StockAdjustmentResponse;
import spring.abtechzone.modules.inventory.dto.response.StockMovementResponse;
import spring.abtechzone.modules.inventory.service.InventoryService;

@WebMvcTest(AdminInventoryController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"app.swagger.enabled=false"})
@AutoConfigureMockMvc(addFilters = false)
class AdminInventoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    InventoryService inventoryService;

    @MockitoBean
    CustomJwtDecoder customJwtDecoder;

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void adminAdjustment_returnsNewBalanceAndMovement() throws Exception {
        StockMovementResponse movement = StockMovementResponse.builder()
                .movementId(11L)
                .skuId(7L)
                .skuCode("SKU-7")
                .changeQty(5)
                .reason(StockMovementReason.PURCHASE_IN)
                .createdBy("admin")
                .createdAt(OffsetDateTime.now())
                .build();
        when(inventoryService.adjustStock(eq(7L), any(StockAdjustmentRequest.class)))
                .thenReturn(StockAdjustmentResponse.builder()
                        .skuId(7L)
                        .onHand(15)
                        .movement(movement)
                        .build());

        mockMvc.perform(post("/admin/inventory/7/adjustments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"operation":"INCREASE","quantity":5,"reason":"PURCHASE_IN"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.onHand").value(15))
                .andExpect(jsonPath("$.result.movement.reason").value("PURCHASE_IN"))
                .andExpect(jsonPath("$.result.movement.createdBy").value("admin"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void invalidAdjustmentBody_returns400() throws Exception {
        mockMvc.perform(post("/admin/inventory/7/adjustments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"operation":"INCREASE","quantity":0,"reason":"PURCHASE_IN"}
								"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "customer", authorities = "ROLE_USER")
    void nonAdminCannotReadMovements() throws Exception {
        mockMvc.perform(get("/admin/inventory/movements")).andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotReadMovements() throws Exception {
        mockMvc.perform(get("/admin/inventory/movements")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", authorities = "ROLE_ADMIN")
    void adminMovementHistory_bindsFiltersAndReturnsPage() throws Exception {
        when(inventoryService.getStockMovements(any(StockMovementSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(StockMovementResponse.builder()
                        .movementId(11L)
                        .skuId(7L)
                        .skuCode("SKU-7")
                        .changeQty(5)
                        .reason(StockMovementReason.PURCHASE_IN)
                        .createdAt(OffsetDateTime.now())
                        .build())));

        mockMvc.perform(get("/admin/inventory/movements")
                        .param("skuId", "7")
                        .param("reason", "PURCHASE_IN")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].skuId").value(7))
                .andExpect(jsonPath("$.result.content[0].reason").value("PURCHASE_IN"));

        verify(inventoryService).getStockMovements(any(StockMovementSearchRequest.class));
    }
}
