package spring.abtechzone.modules.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.dto.request.AddressSearchRequest;
import spring.abtechzone.modules.user.dto.response.AddressResponse;
import spring.abtechzone.modules.user.service.AddressService;

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AddressService addressService;

    @Test
    @DisplayName("POST /addresses - creates address and returns HTTP 200")
    void create_valid_returns200() throws Exception {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest();
        request.setRecipientName("John Doe");
        request.setPhone("0987654321");
        request.setProvince("Hanoi");
        request.setWard("Ward 1");
        request.setStreet("123 Street");
        request.setCountry("Vietnam");

        AddressResponse response = AddressResponse.builder()
                .id(addressId)
                .recipientName("John Doe")
                .phone("0987654321")
                .province("Hanoi")
                .ward("Ward 1")
                .street("123 Street")
                .country("Vietnam")
                .isDefault(true)
                .build();

        when(addressService.create(any(AddressRequest.class))).thenReturn(response);

        mockMvc.perform(post("/addresses")
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(addressId.toString()))
                .andExpect(jsonPath("$.result.recipientName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /addresses - returns paginated addresses")
    void getAddresses_success_returnsPage() throws Exception {
        UUID addressId = UUID.randomUUID();
        AddressResponse response = AddressResponse.builder()
                .id(addressId)
                .recipientName("John Doe")
                .build();

        when(addressService.getAddresses(any(AddressSearchRequest.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/addresses").with(jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].recipientName").value("John Doe"));
    }

    @Test
    @DisplayName("GET /addresses/{addressId} - returns single address")
    void getAddress_success_returnsAddress() throws Exception {
        UUID addressId = UUID.randomUUID();
        AddressResponse response = AddressResponse.builder()
                .id(addressId)
                .recipientName("John Doe")
                .build();

        when(addressService.getAddress(addressId)).thenReturn(response);

        mockMvc.perform(get("/addresses/{addressId}", addressId).with(jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(addressId.toString()));
    }

    @Test
    @DisplayName("PATCH /addresses/{addressId} - updates address")
    void updateAddress_success_returnsUpdatedAddress() throws Exception {
        UUID addressId = UUID.randomUUID();
        AddressRequest request = new AddressRequest();
        request.setRecipientName("Jane Doe");

        AddressResponse response = AddressResponse.builder()
                .id(addressId)
                .recipientName("Jane Doe")
                .build();

        when(addressService.updateAddress(eq(addressId), any(AddressRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/addresses/{addressId}", addressId)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.recipientName").value("Jane Doe"));
    }

    @Test
    @DisplayName("DELETE /addresses/{addressId} - deletes address")
    void deleteAddress_success_returns200() throws Exception {
        UUID addressId = UUID.randomUUID();

        mockMvc.perform(delete("/addresses/{addressId}", addressId)
                        .with(csrf())
                        .with(jwt().jwt(jwt -> jwt.subject("test-user"))))
                .andExpect(status().isOk());

        verify(addressService).deleteAddress(addressId);
    }
}
