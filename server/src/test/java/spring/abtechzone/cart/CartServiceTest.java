package spring.abtechzone.cart;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.UpdateQuantityRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.mapper.CartItemMapper;
import spring.abtechzone.modules.cart.mapper.CartMapper;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.cart.service.CartService;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    CartRepository cartRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    CartItemMapper cartItemMapper;

    @Mock
    CartMapper cartMapper;

    @InjectMocks
    CartService cartService;

    // ── Shared test fixtures ──
    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private User user;
    private ProductSku productSku;
    private Cart cart;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        // Set up SecurityContext — giả lập user đã đăng nhập
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null, List.of()));

        // Shared fixtures
        user = User.builder().id(userId).username("testuser").build();

        Product product = Product.builder().id(1L).name("iPhone 15").build();

        productSku = ProductSku.builder()
                .id(100L)
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(999.99))
                .stock(50)
                .imageUrl("https://example.com/iphone15.png")
                .product(product)
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();

        cartResponse = CartResponse.builder()
                .cartId(1L)
                .userId(userId.toString())
                .status(CartStatus.ACTIVE)
                .items(List.of())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ════════════════════════════════════════════════════════
    //  addToCart
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("addToCart()")
    class AddToCart {

        private CartItemRequest request;

        @BeforeEach
        void setUp() {
            request = CartItemRequest.builder().productSkuId(100L).quantity(2).build();
        }

        @Test
        @DisplayName("Giỏ chưa tồn tại → tạo Cart mới + CartItem mới")
        void shouldCreateNewCartAndNewItem_whenCartDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(productSkuRepository.findById(100L)).thenReturn(Optional.of(productSku));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenReturn(cart);
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(cartResponse);

            // When
            CartResponse result = cartService.addToCart(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCartId()).isEqualTo(1L);

            // Verify Cart mới được tạo và save
            ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(cartCaptor.capture());
            Cart savedCart = cartCaptor.getValue();
            assertThat(savedCart.getUser()).isEqualTo(user);
            assertThat(savedCart.getStatus()).isEqualTo(CartStatus.ACTIVE);

            // Verify CartItem mới được tạo
            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(itemCaptor.capture());
            CartItem savedItem = itemCaptor.getValue();
            assertThat(savedItem.getQuantity()).isEqualTo(2);
            assertThat(savedItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
            assertThat(savedItem.getProductSku()).isEqualTo(productSku);
        }

        @Test
        @DisplayName("Giỏ đã tồn tại + SKU mới → thêm CartItem mới vào giỏ cũ")
        void shouldAddNewItem_whenCartExistsAndSkuIsNew() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(productSkuRepository.findById(100L)).thenReturn(Optional.of(productSku));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(cartResponse);

            // When
            CartResponse result = cartService.addToCart(request);

            // Then
            assertThat(result).isNotNull();

            // Verify KHÔNG tạo Cart mới (dùng cart cũ)
            verify(cartRepository, never()).save(any(Cart.class));

            // Verify CartItem mới được tạo
            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(itemCaptor.capture());
            CartItem savedItem = itemCaptor.getValue();
            assertThat(savedItem.getCart()).isEqualTo(cart);
            assertThat(savedItem.getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("Giỏ đã tồn tại + SKU đã có → cộng dồn quantity, cập nhật giá")
        void shouldAccumulateQuantity_whenSkuAlreadyInCart() {
            // Given — giỏ đã có sản phẩm với quantity = 3
            CartItem existingItem = CartItem.builder()
                    .id(10L)
                    .cart(cart)
                    .productSku(productSku)
                    .quantity(3)
                    .unitPrice(BigDecimal.valueOf(899.99)) // giá cũ
                    .build();
            cart.getItems().add(existingItem);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(productSkuRepository.findById(100L)).thenReturn(Optional.of(productSku));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(cartMapper.toCartResponse(any(Cart.class))).thenReturn(cartResponse);

            // When
            cartService.addToCart(request); // request.quantity = 2

            // Then — quantity phải cộng dồn: 3 + 2 = 5
            assertThat(existingItem.getQuantity()).isEqualTo(5);
            // Giá phải được cập nhật từ productSku (999.99), không giữ giá cũ (899.99)
            assertThat(existingItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.99));

            verify(cartItemRepository).save(existingItem);
        }

        @Test
        @DisplayName("ProductSku không tồn tại → throw SKU_NOT_FOUND")
        void shouldThrowSkuNotFound_whenProductSkuDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(productSkuRepository.findById(100L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.addToCart(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.SKU_NOT_FOUND);
                    });

            // Verify không tương tác với cart
            verify(cartRepository, never()).findByUserIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("User không tồn tại → throw USER_NOT_FOUND")
        void shouldThrowUserNotFound_whenUserDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.addToCart(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    });

            // Verify không tương tác với bất kỳ repository nào khác
            verifyNoInteractions(productSkuRepository, cartRepository, cartItemRepository);
        }
    }

    // ════════════════════════════════════════════════════════
    //  getCart
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getCart()")
    class GetCart {

        @Test
        @DisplayName("Giỏ tồn tại → trả về CartResponse với giá được sync mới nhất")
        void shouldReturnCartAndSyncLatestPrices() {
            // Given — item có giá cũ (899.99), productSku có giá mới (999.99)
            CartItem item = CartItem.builder()
                    .id(10L)
                    .cart(cart)
                    .productSku(productSku) // productSku.price = 999.99
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(899.99)) // giá cũ trong giỏ
                    .build();
            cart.getItems().add(item);

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartMapper.toCartResponse(cart)).thenReturn(cartResponse);

            // When
            CartResponse result = cartService.getCart();

            // Then — giá phải được sync lên 999.99
            assertThat(item.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.99));
            assertThat(result).isEqualTo(cartResponse);
        }

        @Test
        @DisplayName("Giỏ không tồn tại → throw CART_NOT_FOUND")
        void shouldThrowCartNotFound_whenCartDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.getCart())
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND);
                    });
        }
    }

    // ════════════════════════════════════════════════════════
    //  removeCartItem
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("removeCartItem()")
    class RemoveCartItem {

        @Test
        @DisplayName("Item tồn tại → xoá thành công")
        void shouldDeleteCartItem_whenItemExists() {
            // Given
            CartItem cartItem = CartItem.builder()
                    .id(10L)
                    .cart(cart)
                    .productSku(productSku)
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(999.99))
                    .build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductSkuId(1L, 100L)).thenReturn(Optional.of(cartItem));

            // When
            cartService.removeCartItem(100L);

            // Then
            verify(cartItemRepository).delete(cartItem);
        }

        @Test
        @DisplayName("Cart không tồn tại → throw CART_NOT_FOUND")
        void shouldThrowCartNotFound_whenCartDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.removeCartItem(100L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND);
                    });

            verify(cartItemRepository, never()).delete(any(CartItem.class));
        }

        @Test
        @DisplayName("CartItem với skuId không tồn tại → throw CART_ITEM_NOT_FOUND")
        void shouldThrowCartItemNotFound_whenItemDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductSkuId(1L, 999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.removeCartItem(999L))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
                    });

            verify(cartItemRepository, never()).delete(any(CartItem.class));
        }
    }

    // ════════════════════════════════════════════════════════
    //  updateCartItemQuantity
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateCartItemQuantity()")
    class UpdateCartItemQuantity {

        private UpdateQuantityRequest request;
        private CartItem cartItem;

        @BeforeEach
        void setUp() {
            cartItem = CartItem.builder()
                    .id(10L)
                    .cart(cart)
                    .productSku(productSku) // stock = 50, price = 999.99
                    .quantity(2)
                    .unitPrice(BigDecimal.valueOf(899.99)) // giá cũ
                    .build();
        }

        @Test
        @DisplayName("Quantity hợp lệ (≤ stock) → cập nhật thành công + sync giá")
        void shouldUpdateQuantityAndSyncPrice_whenQuantityIsValid() {
            // Given — request quantity = 10, stock = 50 → hợp lệ
            request = UpdateQuantityRequest.builder().quantity(10).build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductSkuId(1L, 100L)).thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CartItemResponse expectedResponse = CartItemResponse.builder()
                    .productSkuId(100L)
                    .skuCode("IPHONE-15-256GB")
                    .productName("iPhone 15")
                    .quantity(10)
                    .unitPrice(BigDecimal.valueOf(999.99))
                    .build();
            when(cartItemMapper.toCartItemResponse(any(CartItem.class))).thenReturn(expectedResponse);

            // When
            CartItemResponse result = cartService.updateCartItemQuantity(100L, request);

            // Then
            assertThat(result.getQuantity()).isEqualTo(10);

            // Verify cartItem đã được cập nhật đúng
            assertThat(cartItem.getQuantity()).isEqualTo(10);
            // Giá phải sync từ productSku (999.99), không giữ giá cũ (899.99)
            assertThat(cartItem.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(999.99));

            verify(cartItemRepository).save(cartItem);
        }

        @Test
        @DisplayName("Quantity vượt stock → throw PRODUCT_STOCK_INVALID")
        void shouldThrowStockInvalid_whenQuantityExceedsStock() {
            // Given — request quantity = 100, stock = 50 → vượt
            request = UpdateQuantityRequest.builder().quantity(100).build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductSkuId(1L, 100L)).thenReturn(Optional.of(cartItem));

            // When & Then
            assertThatThrownBy(() -> cartService.updateCartItemQuantity(100L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_STOCK_INVALID);
                    });

            // Verify không save gì cả
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cart không tồn tại → throw CART_NOT_FOUND")
        void shouldThrowCartNotFound_whenCartDoesNotExist() {
            // Given
            request = UpdateQuantityRequest.builder().quantity(5).build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.updateCartItemQuantity(100L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("CartItem không tồn tại → throw CART_ITEM_NOT_FOUND")
        void shouldThrowCartItemNotFound_whenItemDoesNotExist() {
            // Given
            request = UpdateQuantityRequest.builder().quantity(5).build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndProductSkuId(1L, 999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.updateCartItemQuantity(999L, request))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
                    });
        }
    }

    // ════════════════════════════════════════════════════════
    //  clearCart
    // ════════════════════════════════════════════════════════
    @Nested
    @DisplayName("clearCart()")
    class ClearCart {

        @Test
        @DisplayName("Xoá toàn bộ items trong giỏ thành công")
        void shouldClearAllItemsAndSaveCart() {
            // Given — giỏ có 2 items
            cart.getItems().add(CartItem.builder().id(1L).quantity(2).build());
            cart.getItems().add(CartItem.builder().id(2L).quantity(3).build());

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            // When
            cartService.clearCart();

            // Then — items phải rỗng sau khi clear
            assertThat(cart.getItems()).isEmpty();
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("Cart không tồn tại → throw CART_NOT_FOUND")
        void shouldThrowCartNotFound_whenCartDoesNotExist() {
            // Given
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.clearCart())
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException appEx = (AppException) ex;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.CART_NOT_FOUND);
                    });

            verify(cartRepository, never()).save(any());
        }
    }
}
