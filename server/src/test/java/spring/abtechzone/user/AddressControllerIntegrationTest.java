package spring.abtechzone.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import spring.abtechzone.modules.user.dto.request.AddressRequest;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.repository.UserAddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AddressControllerIntegrationTest {

    @Container
    @SuppressWarnings("rawtypes")
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
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Tên đăng nhập của user chính trong test — phải khớp với JWT subject */
    private String ownerUsername;

    private String otherUsername;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        userAddressRepository.deleteAll();
        userRepository.deleteAll();

        ownerUsername = "owner_" + UUID.randomUUID();
        otherUsername = "other_" + UUID.randomUUID();

        ownerUser = userRepository.save(User.builder()
                .username(ownerUsername)
                .email(ownerUsername + "@test.com")
                .passwordHash("hashed")
                .build());

        userRepository.save(User.builder()
                .username(otherUsername)
                .email(otherUsername + "@test.com")
                .passwordHash("hashed")
                .build());
    }

    // ═══════════════════════════════════════════════
    // POST /addresses — Tạo địa chỉ
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("POST /addresses: tạo địa chỉ thành công, trả về 200 và dữ liệu địa chỉ")
    void createAddress_success() throws Exception {
        AddressRequest request = buildAddressRequest("Hà Nội", true);

        mockMvc.perform(post("/addresses")
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.province").value("Hà Nội"))
                .andExpect(jsonPath("$.result.recipientName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.result.isDefault").value(true));
    }

    @Test
    @DisplayName("POST /addresses: nếu user đã có địa chỉ mặc định, địa chỉ mới KHÔNG được là mặc định")
    void createAddress_whenDefaultAlreadyExists_newAddressShouldNotBeDefault() throws Exception {
        // Tạo địa chỉ mặc định đầu tiên
        mockMvc.perform(post("/addresses")
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddressRequest("Hà Nội", true))))
                .andExpect(status().isOk());

        // Tạo địa chỉ thứ hai yêu cầu isDefault=true
        mockMvc.perform(post("/addresses")
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddressRequest("Đà Nẵng", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isDefault").value(false));
    }

    @Test
    @DisplayName("POST /addresses: không có JWT → 401 Unauthorized")
    void createAddress_whenUnauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddressRequest("Hà Nội", false))))
                .andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════
    // GET /addresses — Danh sách địa chỉ
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("GET /addresses: chỉ trả về địa chỉ của chính user đang đăng nhập")
    void getAddresses_shouldReturnOnlyOwnAddresses() throws Exception {
        saveAddressForUser(ownerUser, "Hà Nội", false);
        saveAddressForUser(ownerUser, "Hải Phòng", true);

        // Tạo thêm 1 địa chỉ cho user khác
        User other = userRepository.findByUsername(otherUsername).orElseThrow();
        saveAddressForUser(other, "TP HCM", false);

        mockMvc.perform(get("/addresses").with(jwt().jwt(jwt -> jwt.subject(ownerUsername))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /addresses: tìm kiếm theo từ khóa province")
    void getAddresses_withSearch_shouldFilterByKeyword() throws Exception {
        saveAddressForUser(ownerUser, "Hà Nội", false);
        saveAddressForUser(ownerUser, "Đà Nẵng", false);

        mockMvc.perform(get("/addresses").param("search", "Đà Nẵng").with(jwt().jwt(jwt -> jwt.subject(ownerUsername))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(1))
                .andExpect(jsonPath("$.result.content[0].province").value("Đà Nẵng"));
    }

    @Test
    @DisplayName("GET /addresses: không có JWT → 401 Unauthorized")
    void getAddresses_whenUnauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/addresses")).andExpect(status().isUnauthorized());
    }

    // ═══════════════════════════════════════════════
    // GET /addresses/{addressId} — Lấy địa chỉ theo id
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("GET /addresses/{id}: lấy địa chỉ thành công khi là chủ sở hữu")
    void getAddress_whenOwner_shouldReturn200() throws Exception {
        UserAddress address = saveAddressForUser(ownerUser, "Cần Thơ", false);

        mockMvc.perform(get("/addresses/{addressId}", address.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.province").value("Cần Thơ"));
    }

    @Test
    @DisplayName("GET /addresses/{id}: trả về 403 khi không phải chủ sở hữu")
    void getAddress_whenNotOwner_shouldReturn403() throws Exception {
        UserAddress address = saveAddressForUser(ownerUser, "Hà Nội", false);

        mockMvc.perform(get("/addresses/{addressId}", address.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(otherUsername))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1061)); // ACCESS_DENIED
    }

    @Test
    @DisplayName("GET /addresses/{id}: trả về 404 khi địa chỉ không tồn tại")
    void getAddress_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/addresses/{addressId}", UUID.randomUUID())
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1035)); // ADDRESS_NOT_FOUND
    }

    // ═══════════════════════════════════════════════
    // PATCH /addresses/{addressId} — Cập nhật địa chỉ
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /addresses/{id}: cập nhật thành công khi là chủ sở hữu")
    void updateAddress_whenOwner_shouldReturn200() throws Exception {
        UserAddress address = saveAddressForUser(ownerUser, "Hà Nội", false);

        AddressRequest updateRequest = buildAddressRequest("TP HCM", false);

        mockMvc.perform(patch("/addresses/{addressId}", address.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.province").value("TP HCM"));
    }

    @Test
    @DisplayName("PATCH /addresses/{id}: trả về 403 khi không phải chủ sở hữu")
    void updateAddress_whenNotOwner_shouldReturn403() throws Exception {
        UserAddress address = saveAddressForUser(ownerUser, "Hà Nội", false);

        mockMvc.perform(patch("/addresses/{addressId}", address.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(otherUsername)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddressRequest("Huế", false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1061)); // ACCESS_DENIED
    }

    @Test
    @DisplayName("PATCH /addresses/{id}: trả về 404 khi địa chỉ không tồn tại")
    void updateAddress_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(patch("/addresses/{addressId}", UUID.randomUUID())
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddressRequest("Huế", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1035)); // ADDRESS_NOT_FOUND
    }

    // ═══════════════════════════════════════════════
    // DELETE /addresses/{addressId} — Xoá địa chỉ
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /addresses/{id}: xoá thành công khi là chủ sở hữu")
    void deleteAddress_whenOwner_shouldReturn200() throws Exception {
        UserAddress address = saveAddressForUser(ownerUser, "Hà Nội", false);

        mockMvc.perform(delete("/addresses/{addressId}", address.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // Xác nhận đã bị xoá khỏi DB
        org.assertj.core.api.Assertions.assertThat(userAddressRepository.findById(address.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("DELETE /addresses/{id}: trả về 403 khi không phải chủ sở hữu")
    void deleteAddress_whenNotOwner_shouldReturn403() throws Exception {
        UserAddress address = saveAddressForUser(ownerUser, "Hà Nội", false);

        mockMvc.perform(delete("/addresses/{addressId}", address.getId())
                        .with(jwt().jwt(jwt -> jwt.subject(otherUsername))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1061)); // ACCESS_DENIED
    }

    @Test
    @DisplayName("DELETE /addresses/{id}: trả về 404 khi địa chỉ không tồn tại")
    void deleteAddress_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/addresses/{addressId}", UUID.randomUUID())
                        .with(jwt().jwt(jwt -> jwt.subject(ownerUsername))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1035)); // ADDRESS_NOT_FOUND
    }

    // ─────────── helpers ───────────

    private AddressRequest buildAddressRequest(String province, boolean isDefault) {
        return AddressRequest.builder()
                .recipientName("Nguyễn Văn A")
                .phone("0901234567")
                .province(province)
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Lợi")
                .country("VN")
                .isDefault(isDefault)
                .build();
    }

    private UserAddress saveAddressForUser(User owner, String province, boolean isDefault) {
        return userAddressRepository.save(UserAddress.builder()
                .user(owner)
                .recipientName("Nguyễn Văn A")
                .phone("0901234567")
                .province(province)
                .district("Quận 1")
                .ward("Phường Bến Nghé")
                .streetAddress("123 Lê Lợi")
                .country("VN")
                .isDefault(isDefault)
                .build());
    }
}
