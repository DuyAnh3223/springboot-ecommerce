package spring.abtechzone.modules.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.user.entity.Address;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.AddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;

class AddressRepositoryIT extends BaseIT {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private spring.abtechzone.modules.order.repository.OrderItemRepository orderItemRepository;

    @Autowired
    private spring.abtechzone.modules.order.repository.OrderRepository orderRepository;

    @Autowired
    private spring.abtechzone.modules.inventory.repository.InventoryReservationRepository
            inventoryReservationRepository;

    @Autowired
    private spring.abtechzone.modules.inventory.repository.StockMovementRepository stockMovementRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        inventoryReservationRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
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

    @Test
    @DisplayName("findByUserId: chỉ trả về địa chỉ của đúng user")
    void findByUserId_shouldReturnOnlyAddressesBelongingToUser() {
        saveAddress(userA, "Hà Nội", false);
        saveAddress(userA, "Hải Phòng", true);
        saveAddress(userB, "TP HCM", false);

        List<Address> result = addressRepository.findByUserId(userA.getId());

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(addr -> addr.getUser().getId().equals(userA.getId()));
    }

    @Test
    @DisplayName("existsByUserIdAndIsDefaultTrue: trả về true khi user đã có địa chỉ mặc định")
    void existsByUserIdAndIsDefaultTrue_whenDefaultExists_shouldReturnTrue() {
        saveAddress(userA, "Hà Nội", true);

        boolean exists = addressRepository.existsByUserIdAndIsDefaultTrue(userA.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("save: lưu địa chỉ và tìm lại theo id thành công")
    void save_andFindById_shouldPersistAddress() {
        Address saved = saveAddress(userA, "Đà Nẵng", false);

        Address found = addressRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getProvince()).isEqualTo("Đà Nẵng");
        assertThat(found.getRecipientName()).isEqualTo("Nguyễn Văn A");
    }

    private Address saveAddress(User owner, String province, boolean isDefault) {
        return addressRepository.save(Address.builder()
                .user(owner)
                .recipientName("Nguyễn Văn A")
                .phone("0901234567")
                .province(province)
                .ward("Phường Bến Nghé")
                .street("123 Lê Lợi")
                .country("VN")
                .isDefault(isDefault)
                .build());
    }
}
