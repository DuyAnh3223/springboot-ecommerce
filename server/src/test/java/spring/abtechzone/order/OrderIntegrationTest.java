package spring.abtechzone.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.inventory.repository.InventoryReservationRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.OrderItemRepository;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.repository.UserAddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrderIntegrationTest {

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
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductSkuRepository productSkuRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    UserAddressRepository userAddressRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    VoucherRepository voucherRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    private User user;
    private Product product;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        // Clean up DB (child to parent)
        stockMovementRepository.deleteAll();
        inventoryReservationRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userAddressRepository.deleteAll();
        voucherRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Seed Category
        Category category = new Category();
        category.setName("Seeded Category");
        category.setSlug("seeded-category");
        category.setIsActive(true);
        category.setSortOrder(1);
        category = categoryRepository.save(category);

        // Create user
        user = userRepository.save(User.builder()
                .username("testuser")
                .passwordHash("password123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .roles(new HashSet<>())
                .build());

        // Create product
        product = productRepository.save(Product.builder()
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max")
                .isPublished(true)
                .isDraft(false)
                .category(category)
                .build());

        // Create SKU
        sku = productSkuRepository.save(ProductSku.builder()
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(1000000.00))
                .stock(50)
                .imageUrl("https://example.com/iphone15.png")
                .product(product)
                .build());
    }

    @Nested
    @DisplayName("POST /orders/checkout-review")
    class CheckoutReview {

        @Test
        @DisplayName("Review cart items successfully without voucher")
        void shouldReviewCartSuccess() throws Exception {
            // Setup cart with item
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(
                    CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

            mockMvc.perform(post("/orders/checkout-review")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.subtotal").value(2000000.00))
                    .andExpect(jsonPath("$.result.shippingFee").value(30000.00))
                    .andExpect(jsonPath("$.result.totalDiscount").value(0.00))
                    .andExpect(jsonPath("$.result.totalCheckout").value(2030000.00))
                    .andExpect(jsonPath("$.result.items.length()").value(1))
                    .andExpect(jsonPath("$.result.items[0].productName").value("iPhone 15 Pro Max"))
                    .andExpect(jsonPath("$.result.items[0].quantity").value(2));
        }

        @Test
        @DisplayName("Review cart items with PERCENTAGE voucher")
        void shouldReviewCartWithVoucherSuccess() throws Exception {
            // Create voucher
            Voucher voucher = voucherRepository.save(Voucher.builder()
                    .name("New Year discount")
                    .code("NEWYEAR10")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10.00))
                    .isActive(true)
                    .applyScope(VoucherApplyScope.ALL)
                    .build());

            // Setup cart
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku)
                    .quantity(3) // 3 * 1000 = 3000
                    .build());

            mockMvc.perform(post("/orders/checkout-review")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                    "voucherCode": "NEWYEAR10"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.subtotal").value(3000000.00))
                    .andExpect(jsonPath("$.result.totalDiscount").value(300000.00)) // 10% of 3000000
                    .andExpect(jsonPath("$.result.totalCheckout").value(2730000.00)); // 3000000 + 30000 - 300000
        }
    }

    @Nested
    @DisplayName("POST /orders")
    class CreateOrder {

        @Test
        @DisplayName("Create order success with new address and saveAddress = true")
        void shouldCreateOrderWithNewAddress() throws Exception {
            // Setup cart
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(
                    CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

            mockMvc.perform(post("/orders")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                    "newUserAddress": {
                                    	"recipientName": "Tran Thi B",
                                    	"phone": "0123456789",
                                    	"province": "Da Nang",
                                    	"district": "Hai Chau",
                                    	"ward": "Thuan Phuoc",
                                    	"streetAddress": "100 Le Loi",
                                    	"saveAddress": true
                                    },
                                    "paymentMethod": "COD"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.orderId").exists())
                    .andExpect(jsonPath("$.result.orderCode").exists())
                    .andExpect(jsonPath("$.result.orderStatus").value("PENDING"))
                    .andExpect(jsonPath("$.result.totalCheckout").value(2030000.00));

            // Verify order in database
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            assertThat(orders).hasSize(1);
            Order savedOrder = orders.get(0);
            assertThat(savedOrder.getRecipientName()).isEqualTo("Tran Thi B");
            assertThat(savedOrder.getPaymentReference()).isEqualTo("COD");
            assertThat(orderItemRepository.findAll()).hasSize(1);

            // Verify address was saved
            List<UserAddress> savedUserAddresses = userAddressRepository.findByUserId(user.getId());
            assertThat(savedUserAddresses).hasSize(1);
            assertThat(savedUserAddresses.get(0).getRecipientName()).isEqualTo("Tran Thi B");

            // Verify stock reduced
            ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
            assertThat(updatedSku.getStock()).isEqualTo(48); // 50 - 2

            // Verify cart status updated to COMPLETED
            Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(CartStatus.COMPLETED);
        }

        @Test
        @DisplayName("Create order success with saved address ID")
        void shouldCreateOrderWithSavedAddress() throws Exception {
            // Setup cart
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(
                    CartItem.builder().cart(cart).productSku(sku).quantity(1).build());

            // Setup address
            UserAddress userAddress = userAddressRepository.save(UserAddress.builder()
                    .recipientName("Le Van C")
                    .phone("0988888888")
                    .province("HCM")
                    .district("Dist 3")
                    .ward("Ward 5")
                    .streetAddress("123 Vo Van Tan")
                    .user(user)
                    .build());

            mockMvc.perform(post("/orders")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                    "addressId": "%s",
                                    "paymentMethod": "BANK_TRANSFER"
                                    }
                                    """.formatted(userAddress.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.orderId").exists());

            // Verify order in DB
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            assertThat(orders).hasSize(1);
            assertThat(orders.get(0).getRecipientName()).isEqualTo("Le Van C");
            assertThat(orders.get(0).getFullAddress()).contains("Vo Van Tan");
        }
    }
}
