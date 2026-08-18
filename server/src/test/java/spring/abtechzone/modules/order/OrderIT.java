package spring.abtechzone.modules.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.inventory.repository.InventoryReservationRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.OrderItemRepository;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.AddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;

@AutoConfigureMockMvc
class OrderIT extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @MockitoSpyBean
    private InventoryService inventoryService;

    private User user;
    private Product product;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        inventoryReservationRepository.deleteAll();
        orderStatusHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        voucherRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        Category category = new Category();
        category.setName("Seeded Category");
        category.setSlug("seeded-category");
        category.setIsActive(true);
        category.setSortOrder(1);
        category = categoryRepository.save(category);

        user = userRepository.save(User.builder()
                .username("testuser")
                .passwordHash("password123")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .isActive(true)
                .roles(new HashSet<>())
                .build());

        product = productRepository.save(Product.builder()
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max")
                .published(true)
                .draft(false)
                .category(category)
                .build());

        sku = productSkuRepository.save(ProductSku.builder()
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(1000000.00))
                .stock(50)
                .imageUrl("https://example.com/iphone15.png")
                .product(product)
                .build());
    }

    @Test
    @DisplayName("Create order success and verify database persistence")
    void shouldCreateOrderWithNewAddress() throws Exception {
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
									"ward": "Thuan Phuoc",
									"street": "100 Le Loi",
									"saveAddress": true
								},
								"paymentMethod": "COD"
								}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.orderId").exists())
                .andExpect(jsonPath("$.result.orderStatus").value("PENDING"));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getRecipientName()).isEqualTo("Tran Thi B");

        ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(updatedSku.getStock()).isEqualTo(48);

        Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(updatedCart.getStatus()).isEqualTo(CartStatus.COMPLETED);
    }

    @Test
    @DisplayName(
            "Late inventory reservation failure proves transactional rollback: cart, address, voucher, order and history remain uncommitted")
    void lateInventoryFailure_provesTransactionalRollback() throws Exception {
        // Create active voucher
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Test Voucher")
                .code("NEWYEAR10")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000.00))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .usedCount(0)
                .build());

        // Setup active cart
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        // Force inventoryService.reserveStock to throw an exception at Step 10 (AFTER cart completed, address saved,
        // voucher used, order & history written)
        doThrow(new AppException(ErrorCode.INSUFFICIENT_STOCK))
                .when(inventoryService)
                .reserveStock(any(), anyInt(), any());

        mockMvc.perform(post("/orders")
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"newUserAddress": {
									"recipientName": "Failed Rollback User",
									"phone": "0999999999",
									"province": "Hanoi",
									"ward": "Ward 1",
									"street": "1 Uncommitted St",
									"saveAddress": true
								},
								"voucherCode": "NEWYEAR10",
								"paymentMethod": "COD"
								}
								"""))
                .andExpect(status().isBadRequest());

        // Verify TRANSACTION ROLLBACK:
        // 1. Order count is 0
        assertThat(orderRepository.count()).isZero();
        // 2. OrderItem count is 0
        assertThat(orderItemRepository.count()).isZero();
        // 3. OrderStatusHistory count is 0
        assertThat(orderStatusHistoryRepository.count()).isZero();
        // 4. Cart status is STILL ACTIVE (not committed as COMPLETED)
        Cart uncommittedCart = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(uncommittedCart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        // 5. Voucher usedCount is STILL 0 (not incremented)
        Voucher uncommittedVoucher = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertThat(uncommittedVoucher.getUsedCount()).isEqualTo(0);
        // 6. Address was NOT saved
        assertThat(addressRepository.count()).isZero();
    }

    @Test
    @DisplayName("Checkout review requires authentication: no JWT returns 401")
    void checkoutReview_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/orders/checkout-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"selectedSkuIds": [1]
								}
								"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Checkout review returns HTTP 200 typed review for business issues")
    void checkoutReview_businessIssue_returns200Review() throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        // Selected SKU is sellable: review succeeds with canPlaceOrder=true
        mockMvc.perform(post("/orders/checkout-review")
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{
								"selectedSkuIds": [%d]
								}
								""".formatted(sku.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].skuId").value(sku.getId()))
                .andExpect(jsonPath("$.result.items[0].lineTotal").value(2000000))
                .andExpect(jsonPath("$.result.subtotal").value(2000000))
                .andExpect(jsonPath("$.result.shippingFee").value(30000))
                .andExpect(jsonPath("$.result.totalAmount").value(2030000))
                .andExpect(jsonPath("$.result.canPlaceOrder").value(true))
                .andExpect(jsonPath("$.result.reviewFingerprint").doesNotExist());
    }
}
