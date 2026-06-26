package spring.abtechzone.voucher;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class VoucherIntegrationTest {

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

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private ProductSku seededSku;

    @BeforeEach
    void initData() {
        voucherRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();

        // Seed a product and sku
        Product product = Product.builder()
                .name("iPhone 15")
                .isDraft(false)
                .isPublished(true)
                .build();
        product = productRepository.save(product);

        ProductSku sku = ProductSku.builder()
                .sku("IPHONE15-128")
                .price(BigDecimal.valueOf(800.00))
                .stock(50)
                .product(product)
                .build();
        seededSku = productSkuRepository.save(sku);
    }

    @Test
    void createVoucher_applyScopeAll_success() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Global Promo")
                .description("10% off everything")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10.0))
                .code("GLOBAL10")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .maxUses(100)
                .maxPerUser(1)
                .minOrderValue(BigDecimal.valueOf(50.0))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.name").value("Global Promo"))
                .andExpect(jsonPath("$.result.code").value("GLOBAL10"))
                .andExpect(jsonPath("$.result.applyScope").value("ALL"));
    }

    @Test
    void createVoucher_applyScopeSpecific_success() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Specific Promo")
                .description("Discount for iPhone 15")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(50.0))
                .code("IPHONE50")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .maxUses(50)
                .maxPerUser(2)
                .minOrderValue(BigDecimal.valueOf(200.0))
                .isActive(true)
                .applyScope(VoucherApplyScope.SPECIFIC)
                .productSkuIds(Set.of(seededSku.getId()))
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.name").value("Specific Promo"))
                .andExpect(jsonPath("$.result.code").value("IPHONE50"))
                .andExpect(jsonPath("$.result.applyScope").value("SPECIFIC"))
                .andExpect(jsonPath("$.result.productSkus[0].sku").value("IPHONE15-128"));
    }

    @Test
    void createVoucher_duplicateCode_throwsException() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Global Promo")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10.0))
                .code("GLOBAL10")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        // First creation succeeds
        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Second creation with same code fails
        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1023))
                .andExpect(jsonPath("$.message").value("Voucher already exists"));
    }

    @Test
    void createVoucher_specificScopeNonExistentSku_throwsException() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Invalid Promo")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(50.0))
                .code("INVALID50")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(5))
                .applyScope(VoucherApplyScope.SPECIFIC)
                .productSkuIds(Set.of(9999L))
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1010))
                .andExpect(jsonPath("$.message").value("SKU not found"));
    }

    @Test
    void getVouchers_success() throws Exception {
        // Create 2 vouchers
        VoucherCreateRequest v1 = VoucherCreateRequest.builder()
                .name("V1")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(5))
                .code("VCODE1")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        VoucherCreateRequest v2 = VoucherCreateRequest.builder()
                .name("V2")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("VCODE2")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v2)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));
    }

    @Test
    void getActiveVouchers_success() throws Exception {
        VoucherCreateRequest active = VoucherCreateRequest.builder()
                .name("Active")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(5))
                .code("ACTIVE")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .isActive(true)
                .build();

        VoucherCreateRequest inactive = VoucherCreateRequest.builder()
                .name("Inactive")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("INACTIVE")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .isActive(false)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(active)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactive)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vouchers/activated")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].code").value("ACTIVE"));
    }

    @Test
    void getVoucher_success() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Promo")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(15))
                .code("PROMO15")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vouchers/PROMO15")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Promo"))
                .andExpect(jsonPath("$.result.code").value("PROMO15"));
    }

    @Test
    void getVoucher_notFound_throwsException() throws Exception {
        mockMvc.perform(get("/vouchers/NONEXISTENT")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1022))
                .andExpect(jsonPath("$.message").value("Voucher not found"));
    }

    @Test
    void updateVoucher_success() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Promo")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(15))
                .code("PROMO15")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        VoucherUpdateRequest updateRequest = VoucherUpdateRequest.builder()
                .name("Updated Promo")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(25))
                .code("PROMO25")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .applyScope(VoucherApplyScope.SPECIFIC)
                .productSkuIds(Set.of(seededSku.getId()))
                .build();

        mockMvc.perform(patch("/vouchers/PROMO15")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.name").value("Updated Promo"))
                .andExpect(jsonPath("$.result.code").value("PROMO25"))
                .andExpect(jsonPath("$.result.value").value(25))
                .andExpect(jsonPath("$.result.applyScope").value("SPECIFIC"))
                .andExpect(jsonPath("$.result.productSkus[0].sku").value("IPHONE15-128"));
    }

    @Test
    void updateVoucher_duplicateCode_throwsException() throws Exception {
        VoucherCreateRequest v1 = VoucherCreateRequest.builder()
                .name("V1")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(5))
                .code("CODE1")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        VoucherCreateRequest v2 = VoucherCreateRequest.builder()
                .name("V2")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("CODE2")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v2)))
                .andExpect(status().isOk());

        VoucherUpdateRequest updateRequest = VoucherUpdateRequest.builder()
                .name("Updated V2")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("CODE1") // conflicts with CODE1
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(patch("/vouchers/CODE2")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1023))
                .andExpect(jsonPath("$.message").value("Voucher already exists"));
    }

    @Test
    void deleteVoucher_success() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Promo")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(15))
                .code("PROMO15")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .isActive(true)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/vouchers/PROMO15")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk());

        // Verify soft-deleted voucher is inactive
        mockMvc.perform(get("/vouchers/PROMO15")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.active").value(false));
    }

    @Test
    void getProductSkusByVoucherCode_applyScopeAll_returnsAllSkus() throws Exception {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Global")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("GLOBAL")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vouchers/GLOBAL/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].sku").value("IPHONE15-128"));
    }

    @Test
    void getProductSkusByVoucherCode_applyScopeSpecific_returnsOnlyAssociatedSkus() throws Exception {
        // Create another SKU not associated with the voucher
        ProductSku sku2 = ProductSku.builder()
                .sku("ANOTHER-SKU")
                .price(BigDecimal.valueOf(100.00))
                .stock(10)
                .product(seededSku.getProduct())
                .build();
        productSkuRepository.save(sku2);

        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Specific")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("SPECIFIC")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.SPECIFIC)
                .productSkuIds(Set.of(seededSku.getId()))
                .build();

        mockMvc.perform(post("/vouchers")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/vouchers/SPECIFIC/products")
                        .with(jwt().jwt(jwt -> jwt.subject("admin").claim("scope", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].sku").value("IPHONE15-128"));
    }
}
