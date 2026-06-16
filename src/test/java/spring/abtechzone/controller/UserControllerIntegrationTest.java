package spring.abtechzone.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import spring.abtechzone.dto.request.UserCreationRequest;
import spring.abtechzone.dto.response.UserResponse;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerIntegrationTest {
    @Container
    static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer("mysql:latest");


//    Connect test database on docker
    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", ()->"com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", ()->"update");

    }

    @Autowired
    private MockMvc mockMvc;

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

//        when(userService.createUser(any())).thenReturn(response);

        // WHEN
        var response = mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.username").value("username"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.firstName").value("firstName"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.result.lastName").value("lastName"));

        log.info("Result: {}",response.andReturn().getResponse().getContentAsString());

        // THEN
    }



}
