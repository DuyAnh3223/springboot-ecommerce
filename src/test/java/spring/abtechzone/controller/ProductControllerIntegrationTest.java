package spring.abtechzone.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.dto.request.ProductRequest;
import spring.abtechzone.dto.request.ProductSkuRequest;
import spring.abtechzone.entity.ProductAttribute;
import spring.abtechzone.repository.ProductRepository;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductControllerIntegrationTest {

    @Container
    static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer("mysql:latest");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MY_SQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MY_SQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MY_SQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductRepository productRepository;

    private ProductRequest request;

    @BeforeEach
    void initData() {
        // Clean up database to ensure isolation between test runs
        productRepository.deleteAll();

        request = ProductRequest.builder()
                .name("Test Product NAME")
                .slug("")
                .description("Test Product DISCRIPTION")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Red", "Blue"))
                        .build()))
                .productSkus(List.of(ProductSkuRequest.builder()
                        .sku("SKU-RED-001")
                        .price(new BigDecimal("99.99"))
                        .stock(100)
                        .imageUrl("http://example.com/red.jpg")
                        .attributes(Map.of("Color", "Red"))
                        .build()))
                .build();
    }

    @Test
    void createProduct_success() throws Exception {
        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.name").value("Test Product NAME"))
                .andExpect(jsonPath("$.result.slug").value("test-product-name"))
                .andExpect(jsonPath("$.result.description").value("Test Product DISCRIPTION"))
                .andExpect(jsonPath("$.result.productSkus[0].sku").value("SKU-RED-001"));
    }

    @Test
    void createProduct_duplicateSku_throwsException() throws Exception {
        // Create the first product successfully
        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Attempting to create another product with the same SKU should fail
        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1009))
                .andExpect(jsonPath("$.message").value("Product SKU already exists"));
    }
}
