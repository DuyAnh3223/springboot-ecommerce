package spring.abtechzone.modules.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.CartMergeItemRequest;
import spring.abtechzone.modules.cart.dto.request.CartMergeRequest;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartMergeLedgerRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.cart.service.CartService;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

class CartPersistenceIT extends BaseIT {

    @Autowired
    private CartService cartService;

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
    private CartMergeLedgerRepository cartMergeLedgerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RedissonClient redissonClient;

    private RLock mergeLock;
    private RBucket<String> mergeBucket;

    private User user;
    private ProductSku productSku;

    @BeforeEach
    void setUp() throws Exception {
        // Clear all tables in correct dependency order
        cartItemRepository.deleteAll();
        cartMergeLedgerRepository.deleteAll();
        cartRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Seed basic data
        Category category = new Category();
        category.setName("Cart IT Category");
        category.setSlug("cart-it-category");
        category.setIsActive(true);
        category.setSortOrder(1);
        category = categoryRepository.save(category);

        user = userRepository.save(User.builder()
                .username("cartuser")
                .passwordHash("hash")
                .email("cart@example.com")
                .firstName("Cart")
                .lastName("User")
                .isActive(true)
                .roles(new HashSet<>())
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Cart Product")
                .slug("cart-product")
                .published(true)
                .draft(false)
                .category(category)
                .build());

        productSku = productSkuRepository.save(ProductSku.builder()
                .sku("CART-SKU-1")
                .price(BigDecimal.valueOf(1000.00))
                .stock(10)
                .imageUrl("https://example.com/image.png")
                .product(product)
                .build());

        when(authService.getCurrentUsername()).thenReturn("cartuser");
        mergeLock = org.mockito.Mockito.mock(RLock.class);
        mergeBucket = org.mockito.Mockito.mock(RBucket.class);
        when(redissonClient.getLock(anyString())).thenReturn(mergeLock);
        when(redissonClient.<String>getBucket(anyString())).thenReturn(mergeBucket);
        when(mergeBucket.get()).thenReturn(null);
        when(mergeLock.tryLock(anyLong(), any())).thenReturn(true);
        when(mergeLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    @DisplayName("getCart() - updates price in database when SKU price changes")
    void getCart_syncsAndPersistsPrice() {
        // 1. Create a Cart and a CartItem with an old price (e.g. 500.00) in database
        Cart cart = cartRepository.save(Cart.builder()
                .user(user)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build());

        CartItem item = cartItemRepository.save(CartItem.builder()
                .cart(cart)
                .productSku(productSku)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(500.00))
                .build());

        // 2. Clear persistence context to start a fresh JPA lifecycle
        entityManager.clear();

        // 3. Call getCart() through CartService (uses transaction)
        CartResponse response = cartService.getCart();

        // 4. Assert that the returned DTO price matches SKU price (1000.00)
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));

        // 5. Clear persistence context again and load CartItem directly from database
        entityManager.clear();
        Optional<CartItem> dbItemOpt = cartItemRepository.findById(item.getId());
        assertThat(dbItemOpt).isPresent();
        assertThat(dbItemOpt.get().getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
    }

    @Test
    @DisplayName("addToCart() - validation failure throws exception and does NOT create a Cart in database")
    void addToCart_validationFailure_doesNotCreateCart() {
        // Ensure no active cart exists
        assertThat(cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE))
                .isEmpty();

        CartItemRequest request = CartItemRequest.builder()
                .productSkuId(productSku.getId())
                .quantity(15) // stock is 10, so quantity 15 exceeds stock
                .build();

        // Call addToCart, expecting failure
        assertThatThrownBy(() -> cartService.addToCart(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_STOCK_INVALID);
                });

        // Verify no cart was created or committed to database
        assertThat(cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE))
                .isEmpty();
    }

    @Test
    @DisplayName("mergeGuestCart() - durable ledger replays without adding quantity twice")
    void mergeGuestCart_replayUsesLedger() {
        var request = CartMergeRequest.builder()
                .mergeId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .items(List.of(CartMergeItemRequest.builder()
                        .skuId(productSku.getId())
                        .quantity(2)
                        .build()))
                .build();

        var first = cartService.mergeGuestCart(request);
        var second = cartService.mergeGuestCart(request);

        assertThat(first.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getStatus().name()).isEqualTo("MERGED");
            assertThat(item.getMergedQuantity()).isEqualTo(2);
        });
        assertThat(second).isEqualTo(first);
        assertThat(cartMergeLedgerRepository.count()).isEqualTo(1);

        entityManager.clear();
        Cart persistedCart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow();
        assertThat(persistedCart.getItems())
                .singleElement()
                .extracting(CartItem::getQuantity)
                .isEqualTo(2);
    }
}
