package spring.abtechzone.modules.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.order.constant.OrderStatus;
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
import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.entity.VoucherRedemption;
import spring.abtechzone.modules.voucher.repository.VoucherRedemptionRepository;
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

    @MockitoSpyBean
    private VoucherRepository voucherRepository;

    @MockitoSpyBean
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private spring.abtechzone.modules.auth.repository.RoleRepository roleRepository;

    @Autowired
    private spring.abtechzone.modules.auth.repository.UserRoleRepository userRoleRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private User user;
    private Product product;
    private ProductSku sku;
    private User admin;

    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final UUID GLOBAL_SCOPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE product_sku SET deleted_at = NULL WHERE deleted_at IS NOT NULL");
        voucherRedemptionRepository.deleteAll();
        stockMovementRepository.deleteAll();
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
        userRoleRepository.deleteAll();
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

        spring.abtechzone.modules.auth.entity.Role adminRole = roleRepository
                .findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(spring.abtechzone.modules.auth.entity.Role.builder()
                        .name("ADMIN")
                        .description("Administrator")
                        .build()));
        admin = userRepository.save(User.builder()
                .username("adminuser")
                .passwordHash(passwordEncoder.encode("Admin123!Pass"))
                .email("admin-it@example.com")
                .firstName("Admin")
                .lastName("User")
                .isActive(true)
                .roles(new HashSet<>())
                .build());
        userRoleRepository.save(spring.abtechzone.modules.auth.entity.UserRole.builder()
                .id(new spring.abtechzone.modules.auth.entity.UserRoleId(
                        admin.getId(), adminRole.getId(), GLOBAL_SCOPE_ID))
                .user(admin)
                .role(adminRole)
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

    private String createOrderBody(String voucherCode, String discountAmount) {
        String voucherJson = voucherCode == null ? "" : """
						,"voucher": {"code": "%s", "applicable": true}
						""".formatted(voucherCode);
        String totalAmount = BigDecimal.valueOf(2030000)
                .subtract(new BigDecimal(discountAmount))
                .toPlainString();
        return """
				{
				"reviewedCheckout": {
					"items": [{"skuId": %d, "quantity": 2, "unitPrice": 1000000, "lineTotal": 2000000}],
					"subtotal": 2000000, "eligibleSubtotal": 2000000, "shippingFee": 30000,
					"discountAmount": %s, "totalAmount": %s%s,
					"canPlaceOrder": true
				},
				"newUserAddress": {
					"recipientName": "Tran Thi B", "phone": "0123456789",
					"province": "Da Nang", "ward": "Thuan Phuoc", "street": "100 Le Loi",
					"saveAddress": false
				},
				"paymentMethod": "COD"
				}
				""".formatted(sku.getId(), discountAmount, totalAmount, voucherJson);
    }

    @Test
    @DisplayName("Create order persists items and SALE_OUT without an inventory reservation table")
    void shouldCreateOrderWithNewAddress() throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody(null, "0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").exists())
                .andExpect(jsonPath("$.result.status").value("PENDING"));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getRecipientName()).isEqualTo("Tran Thi B");
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orders.get(0).getSubtotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000000));
        assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2030000));

        ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(updatedSku.getStock()).isEqualTo(48);

        Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(updatedCart.getStatus()).isEqualTo(CartStatus.COMPLETED);

        // AC-C04-09 / ADR-003: OrderItem is the committed quantity source and
        // StockMovement is the inventory audit; no duplicate reservation table.
        assertThat(orderItemRepository.count()).isEqualTo(1);
        List<StockMovement> movements = stockMovementRepository.findAll();
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getChangeQty()).isEqualTo(-2);
        assertThat(movements.get(0).getReason()).isEqualTo("SALE_OUT");
        assertThat(movements.get(0).getReferenceId())
                .isEqualTo(String.valueOf(orders.get(0).getId()));
        assertThat(tableCount("inventory_reservation")).isZero();
    }

    @Test
    @DisplayName("Same key + same payload replays the same order with no second mutation")
    void sameKeySamePayload_replaysSameOrder() throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        String body = createOrderBody(null, "0");

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Second identical submission with the same key returns the same order, no mutation
        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThat(orderRepository.count()).isEqualTo(1);
        ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(updatedSku.getStock()).isEqualTo(48);
    }

    @Test
    @DisplayName("Same key + different payload returns 409 IDEMPOTENCY_KEY_REUSED with no second order")
    void sameKeyDifferentPayload_returns409() throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody(null, "0")))
                .andExpect(status().isOk());

        // Same key, tampered payload
        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody(null, "50000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.IDEMPOTENCY_KEY_REUSED.getCode()));

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Late voucher failure proves transactional rollback of stock/order/cart AFTER stock decrement")
    void lateVoucherFailure_provesRollbackAfterStockMutation() throws Exception {
        // Voucher must be applied for the flow to reach redeemVoucher.
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Test Voucher")
                .code("ROLLBACK10")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000.00))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .usedCount(0)
                .build());

        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        // Force the voucher redemption to fail AFTER the stock decrement (decreaseStock),
        // and stock movement were written. Only the shared
        // transaction rollback can undo order/cart/stock/voucher together (AC-C04-07).
        doThrow(new AppException(ErrorCode.VOUCHER_ARE_OUT))
                .when(voucherRedemptionRepository)
                .save(any(VoucherRedemption.class));

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody("ROLLBACK10", "100000")))
                .andExpect(status().isBadRequest());

        // Full rollback of every mutated row (AC-C04-07):
        assertThat(orderRepository.count()).isZero();
        assertThat(orderItemRepository.count()).isZero();
        assertThat(orderStatusHistoryRepository.count()).isZero();
        Cart uncommittedCart = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(uncommittedCart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cartItemRepository.findByCartIdAndProductSkuId(cart.getId(), sku.getId()))
                .isPresent();
        // Stock was decremented by the guard, then rolled back:
        ProductSku unchangedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(unchangedSku.getStock()).isEqualTo(50);
        assertThat(stockMovementRepository.count()).isZero();
        Voucher unchangedVoucher = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertThat(unchangedVoucher.getUsedCount()).isZero();
        assertThat(voucherRedemptionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Cart quantity changed while the request waits for the lock is caught by semantic compare")
    void cartChangedWhileWaitingForLock_isCaughtBySemanticCompare() throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        CartItem cartItem = cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        // Change the cart during lock acquisition, before the order transaction starts.
        // The first in-transaction cart load must therefore see quantity 3 and reject
        // the reviewed snapshot, which still says quantity 2.
        RLock lock = redissonClient.getLock("lock:user-order:" + user.getId());
        AtomicBoolean cartChanged = new AtomicBoolean();
        doAnswer(invocation -> {
                    if (cartChanged.compareAndSet(false, true)) {
                        cartItem.setQuantity(3);
                        cartItemRepository.saveAndFlush(cartItem);
                    }
                    return true;
                })
                .when(lock)
                .tryLock(anyLong(), any(TimeUnit.class));

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody(null, "0")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.CHECKOUT_CHANGED.getCode()));

        // No order was created; the cart quantity change was applied outside the flow.
        assertThat(orderRepository.count()).isZero();
        ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(updatedSku.getStock()).isEqualTo(50);
        CartItem updatedCartItem = cartItemRepository
                .findByCartIdAndProductSkuId(cart.getId(), sku.getId())
                .orElseThrow();
        assertThat(updatedCartItem.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Partial selection leaves unselected cart items and keeps cart ACTIVE")
    void partialSelection_leavesUnselectedItems() throws Exception {
        ProductSku sku2 = productSkuRepository.save(ProductSku.builder()
                .sku("ACCESSORY-CASE")
                .price(BigDecimal.valueOf(500000.00))
                .stock(5)
                .product(product)
                .build());

        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku2).quantity(1).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody(null, "0")))
                .andExpect(status().isOk());

        // Only SKU 100 selected -> SKU 200 stays, cart still ACTIVE (AC-C04-08)
        Cart updatedCart = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(updatedCart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cartItemRepository.findByCartIdAndProductSkuId(cart.getId(), sku.getId()))
                .isEmpty();
        assertThat(cartItemRepository.findByCartIdAndProductSkuId(cart.getId(), sku2.getId()))
                .isPresent();

        ProductSku updatedSku2 = productSkuRepository.findById(sku2.getId()).orElseThrow();
        assertThat(updatedSku2.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("Voucher order creates REDEEMED redemption and increments used count")
    void voucherOrder_createsRedemption() throws Exception {
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Test Voucher")
                .code("NEWYEAR10")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000.00))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .usedCount(0)
                .build());

        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody("NEWYEAR10", "100000")))
                .andExpect(status().isOk());

        Voucher updatedVoucher = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertThat(updatedVoucher.getUsedCount()).isEqualTo(1);

        List<VoucherRedemption> redemptions = voucherRedemptionRepository.findAll();
        assertThat(redemptions).hasSize(1);
        assertThat(redemptions.get(0).getStatus().name()).isEqualTo("REDEEMED");
        assertThat(redemptions.get(0).getOrder().getId()).isNotNull();
        assertThat(voucherRedemptionRepository.countByVoucherIdAndUserIdAndStatus(
                        voucher.getId(), user.getId(), VoucherRedemptionStatus.REDEEMED))
                .isEqualTo(1);
        assertThat(tableCount("voucher_user")).isZero();
    }

    @Test
    @DisplayName("REVERSED redemption does not consume maxPerUser for a later order")
    void reversedRedemption_doesNotConsumePerUserLimit() throws Exception {
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Reusable after cancellation")
                .code("REUSE10")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000))
                .maxPerUser(1)
                .usedCount(0)
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .build());

        Cart firstCart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(firstCart).productSku(sku).quantity(2).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody("REUSE10", "100000")))
                .andExpect(status().isOk());

        VoucherRedemption firstRedemption =
                voucherRedemptionRepository.findAll().get(0);
        firstRedemption.setStatus(VoucherRedemptionStatus.REVERSED);
        voucherRedemptionRepository.saveAndFlush(firstRedemption);
        voucher.setUsedCount(0);
        voucherRepository.saveAndFlush(voucher);

        Cart secondCart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(secondCart).productSku(sku).quantity(2).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody("REUSE10", "100000")))
                .andExpect(status().isOk());

        assertThat(voucherRedemptionRepository.countByVoucherIdAndUserIdAndStatus(
                        voucher.getId(), user.getId(), VoucherRedemptionStatus.REDEEMED))
                .isEqualTo(1);
        assertThat(voucherRedemptionRepository.findAll()).hasSize(2);
        assertThat(voucherRepository.findById(voucher.getId()).orElseThrow().getUsedCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("N concurrent submissions with the same idempotency key create exactly one order")
    void concurrentSameKey_createsSingleOrder() throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        String body = createOrderBody(null, "0");
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<org.springframework.mock.web.MockHttpServletResponse> responses =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    responses.add(mockMvc.perform(post("/orders")
                                    .header("Idempotency-Key", IDEMPOTENCY_KEY)
                                    .with(jwt().jwt(j -> j.subject("testuser")))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn()
                            .getResponse());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // AC-C04-10: exactly one order and one stock decrement
        assertThat(orderRepository.count()).isEqualTo(1);
        ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(updatedSku.getStock()).isEqualTo(48);

        // Every thread must have received a response (200 replay or 200 created);
        // no background exception may be swallowed.
        assertThat(responses).hasSize(threads);
        assertThat(responses)
                .allSatisfy(response -> assertThat(response.getStatus()).isEqualTo(200));
    }

    @Test
    @DisplayName("Concurrent different-key orders for the same SKU never oversell")
    void concurrentDifferentKeys_sameSku_doesNotOversell() throws Exception {
        sku.setStock(2);
        productSkuRepository.saveAndFlush(sku);

        int threads = 4;
        List<User> buyers = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            User buyer = userRepository.save(User.builder()
                    .username("concurrent-user-" + i)
                    .passwordHash("password123")
                    .email("concurrent-" + i + "@example.com")
                    .firstName("Concurrent")
                    .lastName("Buyer " + i)
                    .isActive(true)
                    .roles(new HashSet<>())
                    .build());
            Cart cart = cartRepository.save(
                    Cart.builder().user(buyer).status(CartStatus.ACTIVE).build());
            cartItemRepository.save(
                    CartItem.builder().cart(cart).productSku(sku).quantity(2).build());
            buyers.add(buyer);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<org.springframework.mock.web.MockHttpServletResponse> responses =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threads; i++) {
            String key = UUID.randomUUID().toString();
            String username = buyers.get(i).getUsername();
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    responses.add(mockMvc.perform(post("/orders")
                                    .header("Idempotency-Key", key)
                                    .with(jwt().jwt(j -> j.subject(username)))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createOrderBody(null, "0")))
                            .andReturn()
                            .getResponse());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(responses).hasSize(threads);
        assertThat(responses.stream().filter(response -> response.getStatus() == 200))
                .hasSize(1);
        assertThat(responses.stream().filter(response -> response.getStatus() != 200))
                .hasSize(threads - 1)
                .allSatisfy(response -> assertThat(response.getStatus()).isIn(400, 409));
        assertThat(orderRepository.count()).isEqualTo(1);
        ProductSku updatedSku = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(updatedSku.getStock()).isZero();
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

    private int tableCount(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class,
                tableName);
        return count == null ? 0 : count;
    }

    // ────────────────────────────────────────────────────────
    // CP-C05-05/06 — Cancellation compensation exact-once + delivery
    // ────────────────────────────────────────────────────────

    /** Creates an order via the API and returns its code. */
    private String createOrderViaApi(String voucherCode, String discountAmount) throws Exception {
        Cart cart = cartRepository.save(
                Cart.builder().user(user).status(CartStatus.ACTIVE).build());
        cartItemRepository.save(
                CartItem.builder().cart(cart).productSku(sku).quantity(2).build());

        mockMvc.perform(post("/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderBody(voucherCode, discountAmount)))
                .andExpect(status().isOk());

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .get(0)
                .getOrderCode();
    }

    /** Real production-path admin token: signs in via /auth/sign-in with the seeded admin user. */
    private String adminToken() throws Exception {
        String body = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"username": "adminuser", "password": "Admin123!Pass"}
								"""))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.result.token");
    }

    @Test
    @DisplayName("Customer cancel of PENDING order restores stock once and records one history entry")
    void cancelPending_restoresStockExactlyOnce() throws Exception {
        String orderCode = createOrderViaApi(null, "0");
        ProductSku before = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(before.getStock()).isEqualTo(48);

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"reason": "Tôi muốn thay đổi sản phẩm"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CANCELLED"));

        ProductSku after = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(50);

        List<StockMovement> returns = stockMovementRepository.findAll().stream()
                .filter(m -> "ORDER_CANCEL_RETURN".equals(m.getReason()))
                .toList();
        assertThat(returns).hasSize(1);
        assertThat(returns.get(0).getChangeQty()).isEqualTo(2);

        Order cancelled = orderRepository.findByOrderCode(orderCode).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getPaymentStatus().name()).isEqualTo("CANCELLED");
        assertThat(orderStatusHistoryRepository.findByOrderIdOrdered(cancelled.getId()))
                .hasSize(2); // created + cancelled
    }

    @Test
    @DisplayName("Order snapshot remains readable and cancellable after its SKU is renamed and soft-deleted")
    void softDeletedSku_preservesSnapshotAndCancellation() throws Exception {
        String originalSkuCode = sku.getSku();
        String orderCode = createOrderViaApi(null, "0");

        jdbcTemplate.update(
                "UPDATE product_sku SET sku = ?, deleted_at = CURRENT_TIMESTAMP WHERE id = ?",
                "RENAMED-AFTER-ORDER",
                sku.getId());

        mockMvc.perform(get("/orders/{orderCode}", orderCode).with(jwt().jwt(j -> j.subject("testuser"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].skuCode").value(originalSkuCode))
                .andExpect(jsonPath("$.result.items[0].productName").value(product.getName()));

        mockMvc.perform(get("/admin/orders/{orderCode}", orderCode).header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].skuCode").value(originalSkuCode))
                .andExpect(jsonPath("$.result.items[0].productName").value(product.getName()));

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"reason": "cancel after sku deletion"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CANCELLED"));

        Integer restoredStock =
                jdbcTemplate.queryForObject("SELECT stock FROM product_sku WHERE id = ?", Integer.class, sku.getId());
        assertThat(restoredStock).isEqualTo(50);
        assertThat(stockMovementRepository.findAll().stream().filter(m -> "ORDER_CANCEL_RETURN".equals(m.getReason())))
                .hasSize(1);
    }

    @Test
    @DisplayName("Repeated cancel does not restore stock or write a second cancellation history")
    void repeatedCancel_isIdempotent() throws Exception {
        String orderCode = createOrderViaApi(null, "0");
        String body = """
				{"reason": "again"}
				""";

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ProductSku after = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(50);
        assertThat(stockMovementRepository.findAll().stream().filter(m -> "ORDER_CANCEL_RETURN".equals(m.getReason())))
                .hasSize(1);
        Order cancelled = orderRepository.findByOrderCode(orderCode).orElseThrow();
        assertThat(orderStatusHistoryRepository.findByOrderIdOrdered(cancelled.getId()))
                .hasSize(2);
    }

    @Test
    @DisplayName("Cancel reverses voucher redemption exactly once and decrements usedCount once")
    void cancelWithVoucher_reversesExactlyOnce() throws Exception {
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Cancel Voucher")
                .code("CANCEL10")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000.00))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .usedCount(0)
                .build());

        String orderCode = createOrderViaApi("CANCEL10", "100000");

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"reason": "cancel voucher order"}
								"""))
                .andExpect(status().isOk());

        Voucher updated = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertThat(updated.getUsedCount()).isZero();

        List<VoucherRedemption> redemptions = voucherRedemptionRepository.findAll();
        assertThat(redemptions).hasSize(1);
        assertThat(redemptions.get(0).getStatus().name()).isEqualTo("REVERSED");
    }

    @Test
    @DisplayName("Cancellation compensation failure rolls back stock, voucher, order, and history")
    void cancelCompensationFailure_rollsBackWholeTransaction() throws Exception {
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Rollback Cancel Voucher")
                .code("CANCEL-ROLLBACK")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000.00))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .usedCount(0)
                .build());

        String orderCode = createOrderViaApi("CANCEL-ROLLBACK", "100000");
        doThrow(new AppException(ErrorCode.SYSTEM_ERROR))
                .when(voucherRepository)
                .decreaseUsedCount(anyLong());

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"reason": "force compensation rollback"}
								"""))
                .andExpect(status().isInternalServerError());

        Order unchanged = orderRepository.findByOrderCode(orderCode).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(unchanged.getPaymentStatus().name()).isEqualTo("UNPAID");
        assertThat(orderStatusHistoryRepository.findByOrderIdOrdered(unchanged.getId()))
                .hasSize(1); // create history only

        assertThat(productSkuRepository.findById(sku.getId()).orElseThrow().getStock())
                .isEqualTo(48);
        assertThat(stockMovementRepository.findAll().stream().filter(m -> "ORDER_CANCEL_RETURN".equals(m.getReason())))
                .isEmpty();
        assertThat(voucherRepository.findById(voucher.getId()).orElseThrow().getUsedCount())
                .isEqualTo(1);
        List<VoucherRedemption> redemptions = voucherRedemptionRepository.findAll();
        assertThat(redemptions).hasSize(1);
        assertThat(redemptions.get(0).getStatus()).isEqualTo(VoucherRedemptionStatus.REDEEMED);
    }

    @Test
    @DisplayName("Customer cancel of a non-owned order returns 404 without disclosure")
    void cancelNonOwned_returns404() throws Exception {
        String orderCode = createOrderViaApi(null, "0");

        User other = userRepository.save(User.builder()
                .username("other-user")
                .passwordHash("password123")
                .email("other@example.com")
                .firstName("Other")
                .lastName("User")
                .isActive(true)
                .roles(new HashSet<>())
                .build());

        mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                        .with(jwt().jwt(j -> j.subject(other.getUsername())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"reason": "not mine"}
								"""))
                .andExpect(status().isNotFound());

        Order unchanged = orderRepository.findByOrderCode(orderCode).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.PENDING);
        ProductSku after = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(48);
    }

    @Test
    @DisplayName("Two concurrent cancels produce exactly one compensation")
    void concurrentCancels_singleCompensation() throws Exception {
        String orderCode = createOrderViaApi(null, "0");

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<org.springframework.mock.web.MockHttpServletResponse> responses =
                java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    responses.add(mockMvc.perform(post("/orders/{orderCode}/cancel", orderCode)
                                    .with(jwt().jwt(j -> j.subject("testuser")))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
											{"reason": "concurrent"}
											"""))
                            .andReturn()
                            .getResponse());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(responses).hasSize(threads);
        assertThat(responses.stream().filter(r -> r.getStatus() == 200)).hasSize(threads);

        ProductSku after = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(50);
        assertThat(stockMovementRepository.findAll().stream().filter(m -> "ORDER_CANCEL_RETURN".equals(m.getReason())))
                .hasSize(1);
        Order cancelled = orderRepository.findByOrderCode(orderCode).orElseThrow();
        assertThat(orderStatusHistoryRepository.findByOrderIdOrdered(cancelled.getId()))
                .hasSize(2);
    }

    @Test
    @DisplayName("Admin SHIPPING -> DELIVERED marks COD payment PAID; terminal transitions rejected")
    void delivered_setsPaymentPaid_andTerminalIsRejected() throws Exception {
        String orderCode = createOrderViaApi(null, "0");
        String adminBearer = "Bearer " + adminToken();

        // Non-admin (customer JWT) is forbidden from the admin endpoint
        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .with(jwt().jwt(j -> j.subject("testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "CONFIRMED", "note": "confirmed"}
								"""))
                .andExpect(status().isForbidden());

        // Real admin token drives the lifecycle
        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "CONFIRMED", "note": "confirmed"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"));

        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "SHIPPING"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("SHIPPING"));

        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "DELIVERED"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DELIVERED"));

        Order delivered = orderRepository.findByOrderCode(orderCode).orElseThrow();
        assertThat(delivered.getPaymentStatus().name()).isEqualTo("PAID");

        // Terminal state: no further transition
        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "CANCELLED"}
								"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ORDER_STATUS_CONFLICT.getCode()));

        assertThat(orderStatusHistoryRepository.findByOrderIdOrdered(delivered.getId()))
                .hasSize(4); // created + confirmed + shipping + delivered
    }

    @Test
    @DisplayName("Admin cancel of CONFIRMED order restores stock and reverses voucher")
    void adminCancelConfirmed_compensates() throws Exception {
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .name("Admin Cancel Voucher")
                .code("ADMCANCEL10")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(100000.00))
                .isActive(true)
                .applyScope(VoucherApplyScope.ALL)
                .usedCount(0)
                .build());

        String orderCode = createOrderViaApi("ADMCANCEL10", "100000");
        String adminBearer = "Bearer " + adminToken();

        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "CONFIRMED"}
								"""))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/admin/orders/{orderCode}/status", orderCode)
                        .header("Authorization", adminBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
								{"status": "CANCELLED", "note": "admin cancels"}
								"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("CANCELLED"));

        ProductSku after = productSkuRepository.findById(sku.getId()).orElseThrow();
        assertThat(after.getStock()).isEqualTo(50);
        Voucher updatedVoucher = voucherRepository.findById(voucher.getId()).orElseThrow();
        assertThat(updatedVoucher.getUsedCount()).isZero();
        assertThat(voucherRedemptionRepository.findAll().get(0).getStatus().name())
                .isEqualTo("REVERSED");
    }
}
