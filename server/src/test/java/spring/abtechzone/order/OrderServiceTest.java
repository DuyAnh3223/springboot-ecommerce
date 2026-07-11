package spring.abtechzone.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.catalog.entity.Product;
import spring.abtechzone.modules.catalog.entity.ProductSku;
import spring.abtechzone.modules.catalog.repository.ProductSkuRepository;
import spring.abtechzone.modules.order.dto.request.AddressRequest;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.service.OrderService;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.repository.UserAddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    CartRepository cartRepository;

    @Mock
    VoucherRepository voucherRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    UserAddressRepository userAddressRepository;

    @Mock
    VoucherValidator voucherValidator;

    @Mock
    spring.abtechzone.modules.inventory.service.InventoryService inventoryService;

    @Mock
    spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    org.redisson.api.RedissonClient redissonClient;

    @Mock
    org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @org.mockito.Spy
    spring.abtechzone.modules.order.mapper.OrderMapper orderMapper =
            org.mapstruct.factory.Mappers.getMapper(spring.abtechzone.modules.order.mapper.OrderMapper.class);

    @InjectMocks
    OrderService orderService;

    private final java.util.UUID userId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final java.util.UUID addressId = java.util.UUID.fromString("33333333-3333-3333-3333-333333333333");
    private User user;
    private ProductSku sku;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null, List.of()));

        user = User.builder().id(userId).username("testuser").isActive(true).build();

        Product product = Product.builder()
                .id(1L)
                .name("iPhone 15 Pro Max")
                .isPublished(true)
                .build();

        sku = ProductSku.builder()
                .id(100L)
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(1000000.00))
                .stock(10)
                .product(product)
                .build();

        cartItem = CartItem.builder()
                .id(10L)
                .productSku(sku)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(1000000.00))
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();

        lenient()
                .doAnswer(invocation -> {
                    ProductSku skuArg = invocation.getArgument(0);
                    int qty = invocation.getArgument(1);
                    skuArg.setStock(skuArg.getStock() - qty);
                    return null;
                })
                .when(inventoryService)
                .reserveStock(any(), anyInt(), any());

        lenient().when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress addr = invocation.getArgument(0);
            if (addr.getId() == null) {
                addr.setId(java.util.UUID.randomUUID());
            }
            return addr;
        });

        // Mock productSkuRepository findById
        lenient().when(productSkuRepository.findById(anyLong())).thenReturn(Optional.of(sku));

        // Mock redissonClient
        org.redisson.api.RLock mockLock = mock(org.redisson.api.RLock.class);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(mockLock);
        try {
            lenient().when(mockLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            // ignore
        }

        // Mock transactionTemplate
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Nested
    @DisplayName("checkoutReview tests")
    class CheckoutReviewTests {

        @Test
        @DisplayName("checkoutReview success without voucher")
        void reviewSuccess_noVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            CheckoutRequest request = CheckoutRequest.builder().build();
            CheckoutResponse response = orderService.checkoutReview(request);

            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(30000));
            assertThat(response.getTotalDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalCheckout()).isEqualByComparingTo(BigDecimal.valueOf(2030000));
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductName()).isEqualTo("iPhone 15 Pro Max");

            verify(voucherValidator, never()).validateVoucher(any(), any());
        }

        @Test
        @DisplayName("checkoutReview success with PERCENTAGE voucher")
        void reviewSuccess_withPercentageVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            Voucher voucher = Voucher.builder()
                    .code("SALE10")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10.0))
                    .isActive(true)
                    .applyScope(VoucherApplyScope.ALL)
                    .build();

            when(voucherRepository.findByCode("SALE10")).thenReturn(Optional.of(voucher));

            CheckoutRequest request =
                    CheckoutRequest.builder().voucherCode("SALE10").build();
            CheckoutResponse response = orderService.checkoutReview(request);

            assertThat(response.getTotalDiscount()).isEqualByComparingTo(BigDecimal.valueOf(200000.00));
            assertThat(response.getTotalCheckout()).isEqualByComparingTo(BigDecimal.valueOf(2000000 + 30000 - 200000));

            verify(voucherValidator).validateVoucher(voucher, BigDecimal.valueOf(2000000.00));
        }

        @Test
        @DisplayName("checkoutReview throws CART_IS_EMPTY when cart has no items")
        void reviewThrowsCartIsEmpty() {
            cart.setItems(new ArrayList<>());
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            CheckoutRequest request = CheckoutRequest.builder().build();

            assertThatThrownBy(() -> orderService.checkoutReview(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.CART_IS_EMPTY.getMessage());
        }

        @Test
        @DisplayName("checkoutReview throws INSUFFICIENT_STOCK when quantity exceeds stock")
        void reviewThrowsInsufficientStock() {
            cartItem.setQuantity(15); // stock is only 10
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            CheckoutRequest request = CheckoutRequest.builder().build();

            assertThatThrownBy(() -> orderService.checkoutReview(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());
        }
    }

    @Nested
    @DisplayName("createOrder tests")
    class CreateOrderTests {

        @Test
        @DisplayName("createOrder success with Saved Address ID")
        void createOrderSuccess_savedAddress() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            UserAddress userAddress = UserAddress.builder()
                    .id(addressId)
                    .recipientName("Van A")
                    .phone("0909090909")
                    .province("HCM")
                    .district("Dist 1")
                    .ward("Ben Nghe")
                    .streetAddress("1 Le Loi")
                    .user(user)
                    .build();

            when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(userAddress));

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(999L);
                return orderToSave;
            });

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .addressId(addressId)
                    .paymentMethod("COD")
                    .build();

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getOrderId()).isEqualTo(999L);
            assertThat(response.getOrderStatus()).isEqualTo("PENDING");
            assertThat(response.getTotalCheckout()).isEqualByComparingTo(BigDecimal.valueOf(2030000));

            assertThat(sku.getStock()).isEqualTo(8);

            assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);

            verify(orderRepository).save(any(Order.class));
            verify(inventoryService).reserveStock(eq(sku), eq(2), any());
        }

        @Test
        @DisplayName("createOrder success with New Address and saveAddress = true")
        void createOrderSuccess_newAddressAndSave() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            AddressRequest addressReq = AddressRequest.builder()
                    .recipientName("Van B")
                    .phone("0808080808")
                    .province("Da Nang")
                    .district("Hai Chau")
                    .ward("Thach Thang")
                    .streetAddress("50 Nguyen Chi Thanh")
                    .saveAddress(true)
                    .build();

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .newUserAddress(addressReq)
                    .paymentMethod("BANK_TRANSFER")
                    .build();

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(888L);
                return orderToSave;
            });

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getOrderId()).isEqualTo(888L);

            ArgumentCaptor<UserAddress> addressCaptor = ArgumentCaptor.forClass(UserAddress.class);
            verify(userAddressRepository).save(addressCaptor.capture());
            UserAddress savedUserAddress = addressCaptor.getValue();
            assertThat(savedUserAddress.getRecipientName()).isEqualTo("Van B");
            assertThat(savedUserAddress.getUser().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("createOrder throws ADDRESS_REQUIRED when neither addressId nor newAddress is provided")
        void createOrderThrowsAddressRequired() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            CreateOrderRequest request =
                    CreateOrderRequest.builder().paymentMethod("COD").build();

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ADDRESS_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("createOrder throws ADDRESS_NOT_BELONG_TO_USER when user attempts to use other user's address")
        void createOrderThrowsAddressNotBelongToUser() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            User otherUser = User.builder()
                    .id(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .build();
            UserAddress userAddress =
                    UserAddress.builder().id(addressId).user(otherUser).build();

            when(userAddressRepository.findById(addressId)).thenReturn(Optional.of(userAddress));

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .addressId(addressId)
                    .paymentMethod("COD")
                    .build();

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ADDRESS_NOT_BELONG_TO_USER.getMessage());
        }
    }
}
