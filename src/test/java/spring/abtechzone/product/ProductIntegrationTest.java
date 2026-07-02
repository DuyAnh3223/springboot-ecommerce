package spring.abtechzone.product;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import spring.abtechzone.modules.catalog.dto.request.ProductCreateRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductSkuCreateRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductSkuUpdateRequest;
import spring.abtechzone.modules.catalog.dto.request.ProductUpdateRequest;
import spring.abtechzone.modules.catalog.entity.ProductAttribute;
import spring.abtechzone.modules.catalog.repository.ProductRepository;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductIntegrationTest {

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

    private ProductCreateRequest request;

    @BeforeEach
    void initData() {
        // Clean up database to ensure isolation between test runs
        productRepository.deleteAll();

        request = ProductCreateRequest.builder()
                .name("Test Product NAME")
                .description("Test Product DISCRIPTION")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Red", "Blue"))
                        .build()))
                .productSkus(List.of(ProductSkuCreateRequest.builder()
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
    void updateProduct_withProductUpdateRequest_success() throws Exception {
        String response = mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long productId =
                objectMapper.readTree(response).path("result").path("id").asLong();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .name("Updated Product Name")
                .description("Updated product description")
                .thumbnail("http://example.com/updated.jpg")
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Red", "Green"))
                        .build()))
                .build();

        mockMvc.perform(patch("/products/{productId}", productId)
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Product Name"))
                .andExpect(jsonPath("$.result.description").value("Updated product description"))
                .andExpect(jsonPath("$.result.thumbnail").value("http://example.com/updated.jpg"))
                .andExpect(jsonPath("$.result.productSkus[0].sku").value("SKU-RED-001"));
    }

    @Test
    void updateProduct_withAttributesNotMatchingExistingSku_throwsException() throws Exception {
        String response = mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long productId =
                objectMapper.readTree(response).path("result").path("id").asLong();

        ProductUpdateRequest updateRequest = ProductUpdateRequest.builder()
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Green"))
                        .build()))
                .build();

        mockMvc.perform(patch("/products/{productId}", productId)
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011))
                .andExpect(jsonPath("$.message").value("Product attributes do not match existing SKUs"));
    }

    @Test
    void createProduct_withSkuAttributesNotMatchingProductAttributes_throwsException() throws Exception {
        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .name("Invalid Product Attributes")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Red"))
                        .build()))
                .productSkus(List.of(ProductSkuCreateRequest.builder()
                        .sku("SKU-GREEN-INVALID")
                        .price(new BigDecimal("99.99"))
                        .stock(10)
                        .attributes(Map.of("Color", "Green"))
                        .build()))
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011))
                .andExpect(jsonPath("$.message").value("Product attributes do not match existing SKUs"));
    }

    @Test
    void createSku_withAttributesNotMatchingProductAttributes_throwsException() throws Exception {
        ProductCreateRequest productRequest = ProductCreateRequest.builder()
                .name("Product For Invalid SKU")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Red"))
                        .build()))
                .build();

        String productResponse = mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long productId =
                objectMapper.readTree(productResponse).path("result").path("id").asLong();

        ProductSkuCreateRequest invalidSkuRequest = ProductSkuCreateRequest.builder()
                .sku("SKU-BLUE-INVALID")
                .price(new BigDecimal("129.99"))
                .stock(20)
                .attributes(Map.of("Color", "Blue"))
                .build();

        mockMvc.perform(post("/products/{productId}/skus", productId)
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSkuRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011))
                .andExpect(jsonPath("$.message").value("Product attributes do not match existing SKUs"));
    }

    @Test
    void updateSku_withAttributesNotMatchingProductAttributes_throwsException() throws Exception {
        String response = mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long skuId = objectMapper
                .readTree(response)
                .path("result")
                .path("productSkus")
                .get(0)
                .path("id")
                .asLong();

        ProductSkuUpdateRequest updateSkuRequest = ProductSkuUpdateRequest.builder()
                .attributes(Map.of("Color", "Green"))
                .build();

        mockMvc.perform(patch("/products/skus/{skuId}", skuId)
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSkuRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011))
                .andExpect(jsonPath("$.message").value("Product attributes do not match existing SKUs"));
    }

    @Test
    void createAndUpdateSku_withSeparatedSkuRequests_success() throws Exception {
        ProductCreateRequest productRequest = ProductCreateRequest.builder()
                .name("Product Without SKU")
                .description("Product used for SKU endpoint test")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Green", "Blue"))
                        .build()))
                .build();

        String productResponse = mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long productId =
                objectMapper.readTree(productResponse).path("result").path("id").asLong();

        ProductSkuCreateRequest createSkuRequest = ProductSkuCreateRequest.builder()
                .sku("SKU-GREEN-001")
                .price(new BigDecimal("129.99"))
                .stock(20)
                .imageUrl("http://example.com/green.jpg")
                .attributes(Map.of("Color", "Green"))
                .build();

        String skuResponse = mockMvc.perform(post("/products/{productId}/skus", productId)
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSkuRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sku").value("SKU-GREEN-001"))
                .andExpect(jsonPath("$.result.price").value(129.99))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long skuId =
                objectMapper.readTree(skuResponse).path("result").path("id").asLong();

        ProductSkuUpdateRequest updateSkuRequest = ProductSkuUpdateRequest.builder()
                .stock(15)
                .imageUrl("http://example.com/green-updated.jpg")
                .build();

        mockMvc.perform(patch("/products/skus/{skuId}", skuId)
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateSkuRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.sku").value("SKU-GREEN-001"))
                .andExpect(jsonPath("$.result.stock").value(15))
                .andExpect(jsonPath("$.result.imageUrl").value("http://example.com/green-updated.jpg"));
    }

    @Test
    void getProducts_withParameters_success() throws Exception {
        // 1. Seed some products
        ProductCreateRequest product1 = ProductCreateRequest.builder()
                .name("iPhone 15 Pro")
                .description("Latest Apple flagship")
                .isDraft(false)
                .isPublished(true)
                .productSkus(List.of(ProductSkuCreateRequest.builder()
                        .sku("IPHONE15PRO-128")
                        .price(new BigDecimal("999.99"))
                        .stock(50)
                        .imageUrl("http://example.com/iphone15pro.jpg")
                        .build()))
                .build();

        ProductCreateRequest product2 = ProductCreateRequest.builder()
                .name("Samsung Galaxy S24")
                .description("Latest Samsung flagship")
                .isDraft(false)
                .isPublished(true)
                .productSkus(List.of(ProductSkuCreateRequest.builder()
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
        mockMvc.perform(get("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .param("search", "iphone")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.content[0].name").value("iPhone 15 Pro"));

        // 3. Test pagination metadata
        mockMvc.perform(get("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
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
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .param("sortBy", "name")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].name").value("Samsung Galaxy S24"))
                .andExpect(jsonPath("$.result.content[1].name").value("iPhone 15 Pro"));

        // 5. Test pagination (size=1, page=2 sorted by name asc)
        mockMvc.perform(get("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
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

    @Test
    void createProduct_withDuplicateAttributeNames_throwsException() throws Exception {
        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .name("Invalid Product Duplicate Attributes")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(
                        ProductAttribute.builder()
                                .name("Color")
                                .values(List.of("Red"))
                                .build(),
                        ProductAttribute.builder()
                                .name("Color")
                                .values(List.of("Blue"))
                                .build()))
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011))
                .andExpect(jsonPath("$.message").value("Product attributes do not match existing SKUs"));
    }

    @Test
    void createProduct_withDuplicateAttributeValues_throwsException() throws Exception {
        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .name("Invalid Product Duplicate Values")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name("Color")
                        .values(List.of("Red", "Red"))
                        .build()))
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011));
    }

    @Test
    void createProduct_withBlankAttributeName_throwsException() throws Exception {
        ProductCreateRequest invalidRequest = ProductCreateRequest.builder()
                .name("Invalid Product Blank Name")
                .isDraft(false)
                .isPublished(true)
                .attributes(List.of(ProductAttribute.builder()
                        .name(" ")
                        .values(List.of("Red"))
                        .build()))
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1011));
    }

    @Test
    void createProduct_duplicateSlug_throwsException() throws Exception {
        // Create first product
        ProductCreateRequest p1 = ProductCreateRequest.builder()
                .name("Same Product Name")
                .isDraft(false)
                .isPublished(true)
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isOk());

        // Attempt second product with same name (generates duplicate slug)
        ProductCreateRequest p2 = ProductCreateRequest.builder()
                .name("Same Product Name")
                .isDraft(false)
                .isPublished(true)
                .build();

        mockMvc.perform(post("/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018))
                .andExpect(jsonPath("$.message").value("Product slug already exists"));
    }
}
