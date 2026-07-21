package spring.abtechzone.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.repository.UserAddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AddressRepositoryIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("db/init-extensions.sql");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private UserAddressRepository userAddressRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userAddressRepository.deleteAll();
        userRepository.deleteAll();

        userA = userRepository.save(User.builder()
                .username("user_a_" + UUID.randomUUID())
                .email("user_a_" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .build());

        userB = userRepository.save(User.builder()
                .username("user_b_" + UUID.randomUUID())
                .email("user_b_" + UUID.randomUUID() + "@test.com")
                .passwordHash("hashed")
                .build());
    }

    // ─────────── findByUserId ───────────

    @Test
    @DisplayName("findByUserId: chỉ trả về địa chỉ của đúng user")
    void findByUserId_shouldReturnOnlyAddressesBelongingToUser() {
        saveAddress(userA, "Hà Nội", false);
        saveAddress(userA, "Hải Phòng", true);
        saveAddress(userB, "TP HCM", false);

        List<UserAddress> result = userAddressRepository.findByUserId(userA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(addr -> addr.getUser().getId().equals(userA.getId()));
    }

    @Test
    @DisplayName("findByUserId: trả về danh sách rỗng khi user chưa có địa chỉ nào")
    void findByUserId_whenNoAddress_shouldReturnEmptyList() {
        List<UserAddress> result = userAddressRepository.findByUserId(userA.getId());

        assertThat(result).isEmpty();
    }

    // ─────────── existsByUserIdAndIsDefaultTrue ───────────

    @Test
    @DisplayName("existsByUserIdAndIsDefaultTrue: trả về true khi user đã có địa chỉ mặc định")
    void existsByUserIdAndIsDefaultTrue_whenDefaultExists_shouldReturnTrue() {
        saveAddress(userA, "Hà Nội", true);

        boolean exists = userAddressRepository.existsByUserIdAndIsDefaultTrue(userA.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndIsDefaultTrue: trả về false khi user chưa có địa chỉ mặc định")
    void existsByUserIdAndIsDefaultTrue_whenNoDefault_shouldReturnFalse() {
        saveAddress(userA, "Hà Nội", false);

        boolean exists = userAddressRepository.existsByUserIdAndIsDefaultTrue(userA.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByUserIdAndIsDefaultTrue: địa chỉ mặc định của userB không ảnh hưởng userA")
    void existsByUserIdAndIsDefaultTrue_defaultOfOtherUser_shouldNotAffect() {
        saveAddress(userB, "TP HCM", true);

        boolean exists = userAddressRepository.existsByUserIdAndIsDefaultTrue(userA.getId());

        assertThat(exists).isFalse();
    }

    // ─────────── save / findById ───────────

    @Test
    @DisplayName("save: lưu địa chỉ và tìm lại theo id thành công")
    void save_andFindById_shouldPersistAddress() {
        UserAddress saved = saveAddress(userA, "Đà Nẵng", false);

        UserAddress found = userAddressRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getProvince()).isEqualTo("Đà Nẵng");
        assertThat(found.getRecipientName()).isEqualTo("Nguyễn Văn A");
        assertThat(found.getIsDefault()).isFalse();
    }

    @Test
    @DisplayName("delete: xoá địa chỉ, findById trả về empty")
    void delete_shouldRemoveAddress() {
        UserAddress saved = saveAddress(userA, "Cần Thơ", false);

        userAddressRepository.delete(saved);

        assertThat(userAddressRepository.findById(saved.getId())).isEmpty();
    }

    // ─────────── helper ───────────

    private UserAddress saveAddress(User owner, String province, boolean isDefault) {
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
