package spring.abtechzone.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getProducts_withParameters_success() throws Exception {
        // 1. Seed some products
        ProductRequest product1 = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .slug("")
                .description("Latest Apple flagship")
                .isDraft(false)
                .isPublished(true)
                .productSkus(List.of(ProductSkuRequest.builder()
                        .sku("IPHONE15PRO-128")
                        .price(new BigDecimal("999.99"))
                        .stock(50)
                        .imageUrl("http://example.com/iphone15pro.jpg")
                        .build()))
                .build();

        ProductRequest product2 = ProductRequest.builder()
                .name("Samsung Galaxy S24")
                .slug("")
                .description("Latest Samsung flagship")
                .isDraft(false)
                .isPublished(true)
                .productSkus(List.of(ProductSkuRequest.builder()
                        .sku("SAMSUNG-S24-128")
                        .price(new BigDecimal("799.99"))
                        .stock(100)
                        .imageUrl("http://example.com/samsung_s24.jpg")
                        .build()))
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product2)))
                .andExpect(status().isOk());

        // 2. Test filter by name (keyword)
        mockMvc.perform(get("/products").param("search", "iphone").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].name").value("iPhone 15 Pro"));

        // 3. Test pagination metadata
        mockMvc.perform(get("/products")
                        .param("search", "flagship")
                        .param("size", "1")
                        .param("page", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(2))
                .andExpect(jsonPath("$.result.size").value(1))
                .andExpect(jsonPath("$.result.number").value(0));

        // 4. Test sorting by name desc
        mockMvc.perform(get("/products")
                        .param("sortBy", "name")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].name").value("Samsung Galaxy S24"))
                .andExpect(jsonPath("$.result.content[1].name").value("iPhone 15 Pro"));

        // 5. Test pagination (size=1, page=2 sorted by name asc)
        mockMvc.perform(get("/products")
                        .param("sortBy", "name")
                        .param("order", "asc")
                        .param("size", "1")
                        .param("page", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].name").value("Samsung Galaxy S24"))
                .andExpect(jsonPath("$.result.number").value(1));
    }
}
