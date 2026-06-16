package spring.abtechzone.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import spring.abtechzone.dto.request.UserCreationRequest;
import spring.abtechzone.dto.response.UserResponse;
import spring.abtechzone.service.UserService;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserCreationRequest request;
    private UserResponse response;

    @BeforeEach
    void initData(){
        request = UserCreationRequest.builder()
                .username("username")
                .password("password")
                .firstName("firstName")
                .lastName("lastName")
                .build();

        response = UserResponse.builder()
                .id("dwqsadsadg563433")
                .username("username")
                .firstName("firstName")
                .lastName("lastName")
                .build();
    }

    @Test
    //
    void createUser_validRequest_success() throws Exception {
        // GIVEN
        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(request);

        when(userService.createUser(any())).thenReturn(response);

        // WHEN
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code")
                        .value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.id")
                        .value("dwqsadsadg563433")
        );

        // THEN
    }

    @Test
        //
    void createUser_usernameInvalid_fail() throws Exception {
        // GIVEN
        request.setUsername("us");
        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(request);

//        Mockito.when(userService.createUser(any())).thenReturn(response);

        // WHEN
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code")
                        .value(1003))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message")
                        .value("Username must be at least 3 characters")
                );

        // THEN
    }

}
