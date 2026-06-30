package spring.abtechzone.cart;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

/**
 * Integration Test cho Cart module.
 * <p>
 * Test toàn bộ luồng: HTTP Request → Controller → Service → Repository → MySQL (Testcontainers)
 * <p>
 * Khác với Unit Test (CartServiceTest):
 * - Spring Context thật, MySQL DB thật (Testcontainers), Security thật
 * - Gọi qua HTTP (MockMvc), verify JSON response + DB state
 * - Dùng .with(jwt()) để bypass CustomJwtDecoder
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CartIntegrationTest {

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

    // ── Shared test data ──
    private User user;
    private Product product;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        // Clean up — theo thứ tự dependency (con trước, cha sau)
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Tạo User thật trong MySQL
        user = userRepository.save(User.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .roles(new HashSet<>())
                .build());

        // Tạo Product + SKU thật trong MySQL
        product = productRepository.save(Product.builder()
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max")
                .isPublished(true)
                .isDraft(false)
                .build());

        sku = productSkuRepository.save(ProductSku.builder()
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(999.99))
                .stock(50)
                .imageUrl("https://example.com/iphone15.png")
                .product(product)
                .build());
    }

    // ════════════════════════════════════════════════════════
    //  POST /cart/add
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /cart/add")
    class AddToCart {

        @Test
        @DisplayName("Thêm sản phẩm mới → 200 OK + Cart được tạo trong DB")
        void shouldAddItemAndCreateCart() throws Exception {
            mockMvc.perform(post("/cart/add")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"productSkuId": %d, "quantity": 2}
									""".formatted(sku.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.cartId").exists())
                    .andExpect(jsonPath("$.result.userId").value(user.getId()))
                    .andExpect(jsonPath("$.result.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.result.items").isArray())
                    .andExpect(jsonPath("$.result.items.length()").value(1))
                    .andExpect(jsonPath("$.result.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.result.items[0].productSkuId").value(sku.getId()))
                    .andExpect(jsonPath("$.result.items[0].unitPrice").value(999.99));

            // Verify DB — Cart thực sự được lưu
            Cart savedCart = cartRepository.findByUserId(user.getId()).orElseThrow();
            assertThat(cartItemRepository.findAll()).hasSize(1);
            assertThat(savedCart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        }

        @Test
        @DisplayName("Thêm SKU đã có trong giỏ → cộng dồn quantity")
        void shouldAccumulateQuantity_whenSkuAlreadyInCart() throws Exception {
            // Setup — tạo cart có sẵn item với quantity = 3
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku)
                    .quantity(3)
                    .unitPrice(BigDecimal.valueOf(999.99))
                    .build());

            // Thêm lần 2 (quantity = 2) → kỳ vọng tổng = 5
            mockMvc.perform(post("/cart/add")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"productSkuId": %d, "quantity": 2}
									""".formatted(sku.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.items.length()").value(1))
                    .andExpect(jsonPath("$.result.items[0].quantity").value(5));
        }

        @Test
        @DisplayName("SKU không tồn tại → 404 NOT_FOUND")
        void shouldReturn404_whenSkuNotFound() throws Exception {
            mockMvc.perform(post("/cart/add")
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"productSkuId": 999999, "quantity": 1}
									"""))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(1010))
                    .andExpect(jsonPath("$.message").value("SKU not found"));
        }

        @Test
        @DisplayName("Không có JWT → 401 Unauthorized")
        void shouldReturn401_whenNoToken() throws Exception {
            mockMvc.perform(post("/cart/add")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"productSkuId": 1, "quantity": 1}
									"""))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ════════════════════════════════════════════════════════
    //  GET /cart
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /cart")
    class GetCart {

        @Test
        @DisplayName("Giỏ tồn tại → 200 OK + trả đúng dữ liệu")
        void shouldReturnCart() throws Exception {
            // Setup
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku)
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(899.99)) // giá cũ
                    .build());

            mockMvc.perform(get("/cart").with(jwt().jwt(j -> j.subject("testuser"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.cartId").value(cart.getId()))
                    .andExpect(jsonPath("$.result.items.length()").value(1))
                    .andExpect(jsonPath("$.result.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.result.items[0].productName").value("iPhone 15 Pro Max"));
        }

        @Test
        @DisplayName("Giỏ không tồn tại → 404 NOT_FOUND")
        void shouldReturn404_whenCartNotFound() throws Exception {
            mockMvc.perform(get("/cart").with(jwt().jwt(j -> j.subject("testuser"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(1030))
                    .andExpect(jsonPath("$.message").value("Cart not found"));
        }
    }

    // ════════════════════════════════════════════════════════
    //  DELETE /cart/items/{skuId}
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /cart/items/{skuId}")
    class RemoveCartItem {

        @Test
        @DisplayName("Xoá item tồn tại → 200 OK + item bị xoá khỏi DB")
        void shouldRemoveItemFromCart() throws Exception {
            // Setup
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            CartItem item = cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku)
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(999.99))
                    .build());

            mockMvc.perform(delete("/cart/items/{skuId}", sku.getId()).with(jwt().jwt(j -> j.subject("testuser"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cart item removed successfully"));

            // Verify DB — item thực sự bị xoá
            assertThat(cartItemRepository.findById(item.getId())).isEmpty();
        }

        @Test
        @DisplayName("SKU không có trong giỏ → 404 CART_ITEM_NOT_FOUND")
        void shouldReturn404_whenItemNotInCart() throws Exception {
            // Setup — tạo cart trống
            cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());

            mockMvc.perform(delete("/cart/items/{skuId}", 999999L).with(jwt().jwt(j -> j.subject("testuser"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(1028));
        }
    }

    // ════════════════════════════════════════════════════════
    //  PATCH /cart/items/{skuId}
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PATCH /cart/items/{skuId}")
    class UpdateCartItemQuantity {

        private Cart cart;

        @BeforeEach
        void setUpCart() {
            cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku) // stock = 50
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(999.99))
                    .build());
        }

        @Test
        @DisplayName("Quantity hợp lệ (≤ stock) → 200 OK + quantity được cập nhật")
        void shouldUpdateQuantity_whenValid() throws Exception {
            mockMvc.perform(patch("/cart/items/{skuId}", sku.getId())
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"quantity": 10}
									"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1000))
                    .andExpect(jsonPath("$.result.quantity").value(10))
                    .andExpect(jsonPath("$.result.productSkuId").value(sku.getId()));
        }

        @Test
        @DisplayName("Quantity vượt stock → 400 PRODUCT_STOCK_INVALID")
        void shouldReturn400_whenQuantityExceedsStock() throws Exception {
            mockMvc.perform(patch("/cart/items/{skuId}", sku.getId())
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"quantity": 100}
									"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(1015));
        }

        @Test
        @DisplayName("Quantity = 0 (vi phạm @Min(1)) → 400 Validation Error")
        void shouldReturn400_whenQuantityIsZero() throws Exception {
            mockMvc.perform(patch("/cart/items/{skuId}", sku.getId())
                            .with(jwt().jwt(j -> j.subject("testuser")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
									{"quantity": 0}
									"""))
                    .andExpect(status().isBadRequest());
        }
    }

    // ════════════════════════════════════════════════════════
    //  DELETE /cart
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /cart")
    class ClearCart {

        @Test
        @DisplayName("Xoá toàn bộ giỏ → 200 OK + items rỗng trong DB")
        void shouldClearAllItems() throws Exception {
            // Setup — giỏ có 2 items
            Cart cart = cartRepository.save(
                    Cart.builder().user(user).status(CartStatus.ACTIVE).build());

            ProductSku sku2 = productSkuRepository.save(ProductSku.builder()
                    .sku("IPHONE-15-512GB")
                    .price(BigDecimal.valueOf(1199.99))
                    .stock(30)
                    .product(product)
                    .build());

            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku)
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(999.99))
                    .build());
            cartItemRepository.save(CartItem.builder()
                    .cart(cart)
                    .productSku(sku2)
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(1199.99))
                    .build());

            mockMvc.perform(delete("/cart").with(jwt().jwt(j -> j.subject("testuser"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cart cleared successfully"));

            // Verify DB — cart vẫn tồn tại nhưng items rỗng
            Cart savedCart = cartRepository.findByUserId(user.getId()).orElseThrow();
            assertThat(cartItemRepository.findAll()).isEmpty();
        }
    }
}
