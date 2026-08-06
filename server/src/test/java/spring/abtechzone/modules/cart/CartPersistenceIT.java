package spring.abtechzone.modules.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
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
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;


    @MockitoBean
    private AuthService authService;

    private User user;
    private ProductSku productSku;

    @BeforeEach
    void setUp() {
        // Clear all tables in correct dependency order
        cartItemRepository.deleteAll();
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
}
