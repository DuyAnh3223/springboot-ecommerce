package spring.abtechzone.modules.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.dto.request.AddressRequest;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.mapper.OrderMapper;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.order.service.OrderService;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.Address;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.AddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.service.VoucherService;
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
    AddressRepository addressRepository;

    @Mock
    VoucherValidator voucherValidator;

    @Mock
    InventoryService inventoryService;

    @Mock
    OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    RedissonClient redissonClient;

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    AuthService authService;

    @Mock
    VoucherService voucherService;

    @Spy
    OrderMapper orderMapper = Mappers.getMapper(spring.abtechzone.modules.order.mapper.OrderMapper.class);

    @InjectMocks
    OrderService orderService;

    private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID addressId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private User user;
    private ProductSku sku;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null, List.of()));
        lenient().when(authService.getCurrentUsername()).thenReturn("testuser");

        user = User.builder().id(userId).username("testuser").isActive(true).build();

        Product product = Product.builder()
                .id(1L)
                .name("iPhone 15 Pro Max")
                .published(true)
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

        lenient().when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address addr = invocation.getArgument(0);
            if (addr.getId() == null) {
                addr.setId(java.util.UUID.randomUUID());
            }
            return addr;
        });

        lenient().when(productSkuRepository.findById(anyLong())).thenReturn(Optional.of(sku));

        RLock mockLock = mock(org.redisson.api.RLock.class);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(mockLock);
        try {
            lenient().when(mockLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            // ignore
        }

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        lenient()
                .when(voucherService.calculateEligibleSubtotal(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Voucher v = invocation.getArgument(0);
                    @SuppressWarnings("unchecked")
                    Map<Long, BigDecimal> skuSubtotals = (Map<Long, BigDecimal>) invocation.getArgument(1);
                    BigDecimal fullSubtotal = invocation.getArgument(2);
                    if (v == null || v.getApplyScope() == null || v.getApplyScope() == VoucherApplyScope.ALL) {
                        return fullSubtotal != null ? fullSubtotal : BigDecimal.ZERO;
                    }
                    if (skuSubtotals == null || v.getProductSkus() == null) {
                        return BigDecimal.ZERO;
                    }
                    Set<Long> eligibleIds =
                            v.getProductSkus().stream().map(ProductSku::getId).collect(Collectors.toSet());
                    BigDecimal sum = BigDecimal.ZERO;
                    for (Map.Entry<Long, BigDecimal> entry : skuSubtotals.entrySet()) {
                        if (eligibleIds.contains(entry.getKey())) {
                            sum = sum.add(entry.getValue());
                        }
                    }
                    return sum;
                });

        lenient().when(voucherService.getDiscount(any(), any())).thenAnswer(invocation -> {
            Voucher v = invocation.getArgument(0);
            BigDecimal eligible = invocation.getArgument(1);
            if (v == null || eligible == null || eligible.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal voucherVal = v.getValue() != null ? v.getValue() : BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            if (v.getType() == VoucherType.FIXED_AMOUNT) {
                discount = voucherVal.min(eligible);
            } else if (v.getType() == VoucherType.PERCENTAGE) {
                discount = eligible.multiply(voucherVal)
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                if (v.getMaxDiscountAmount() != null && v.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    discount = discount.min(v.getMaxDiscountAmount());
                }
            }
            if (discount.compareTo(eligible) > 0) {
                discount = eligible;
            }
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                discount = BigDecimal.ZERO;
            }
            return discount.setScale(2, java.math.RoundingMode.HALF_UP);
        });
    }

    @Nested
    @DisplayName("checkoutReview tests")
    class CheckoutReviewTests {

        @Test
        @DisplayName("checkoutReview success without voucher")
        void reviewSuccess_noVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CheckoutRequest request = CheckoutRequest.builder().build();
            CheckoutResponse response = orderService.checkoutReview(request);

            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(30000));
            assertThat(response.getTotalDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalCheckout()).isEqualByComparingTo(BigDecimal.valueOf(2030000));
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductName()).isEqualTo("iPhone 15 Pro Max");

            verify(voucherValidator, never()).validateForCheckout(any(), any(), any(), any());
        }

        @Test
        @DisplayName("checkoutReview success with PERCENTAGE voucher")
        void reviewSuccess_withPercentageVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

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

            verify(voucherValidator)
                    .validateForCheckout(
                            eq(voucher),
                            eq(user),
                            eq(BigDecimal.valueOf(2000000.00)),
                            eq(BigDecimal.valueOf(2000000.00)));
        }

        @Test
        @DisplayName("checkoutReview success with SPECIFIC scope voucher discounting only eligible SKU")
        void reviewSuccess_withSpecificVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            ProductSku sku2 = ProductSku.builder()
                    .id(200L)
                    .sku("ACCESSORY-CASE")
                    .price(BigDecimal.valueOf(500000.00))
                    .stock(5)
                    .product(Product.builder()
                            .id(2L)
                            .name("Case")
                            .published(true)
                            .build())
                    .build();

            CartItem item2 = CartItem.builder()
                    .id(20L)
                    .productSku(sku2)
                    .quantity(1)
                    .unitPrice(BigDecimal.valueOf(500000.00))
                    .build();

            cart.getItems().add(item2);
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            Voucher voucher = Voucher.builder()
                    .code("SPECIFIC10")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10.0))
                    .isActive(true)
                    .applyScope(VoucherApplyScope.SPECIFIC)
                    .productSkus(Set.of(sku))
                    .build();

            when(voucherRepository.findByCode("SPECIFIC10")).thenReturn(Optional.of(voucher));

            CheckoutRequest request =
                    CheckoutRequest.builder().voucherCode("SPECIFIC10").build();
            CheckoutResponse response = orderService.checkoutReview(request);

            // Subtotal = 2.000.000 (sku 100) + 500.000 (sku 200) = 2.500.000
            // Discount = 10% of 2.000.000 (eligible SKU only) = 200.000
            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2500000.00));
            assertThat(response.getTotalDiscount()).isEqualByComparingTo(BigDecimal.valueOf(200000.00));
            assertThat(response.getTotalCheckout()).isEqualByComparingTo(BigDecimal.valueOf(2500000 + 30000 - 200000));

            verify(voucherValidator)
                    .validateForCheckout(
                            eq(voucher),
                            eq(user),
                            eq(BigDecimal.valueOf(2500000.00)),
                            eq(BigDecimal.valueOf(2000000.00)));
        }

        @Test
        @DisplayName("checkoutReview throws VOUCHER_PER_USER_LIMIT_REACHED when validation fails")
        void reviewThrows_perUserLimitReached() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            Voucher voucher = Voucher.builder()
                    .id(50L)
                    .code("ONCE_ONLY")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .isActive(true)
                    .maxPerUser(1)
                    .applyScope(VoucherApplyScope.ALL)
                    .build();

            when(voucherRepository.findByCode("ONCE_ONLY")).thenReturn(Optional.of(voucher));
            doThrow(new AppException(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED))
                    .when(voucherValidator)
                    .validateForCheckout(any(), any(), any(), any());

            CheckoutRequest request =
                    CheckoutRequest.builder().voucherCode("ONCE_ONLY").build();

            assertThatThrownBy(() -> orderService.checkoutReview(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED.getMessage());
        }

        @Test
        @DisplayName("checkoutReview throws CART_IS_EMPTY when cart has no items")
        void reviewThrowsCartIsEmpty() {
            cart.setItems(new ArrayList<>());
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

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
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

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
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            Address address = Address.builder()
                    .id(addressId)
                    .recipientName("Van A")
                    .phone("0909090909")
                    .province("HCM")
                    .ward("Ben Nghe")
                    .street("1 Le Loi")
                    .user(user)
                    .build();

            when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

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

            assertThat(cart.getStatus()).isEqualTo(CartStatus.COMPLETED);

            verify(orderRepository).save(any(Order.class));
            verify(inventoryService).reserveStock(eq(sku), eq(2), any());
        }

        @Test
        @DisplayName("createOrder success with New Address and saveAddress = true")
        void createOrderSuccess_newAddressAndSave() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            AddressRequest addressReq = AddressRequest.builder()
                    .recipientName("Van B")
                    .phone("0808080808")
                    .province("Da Nang")
                    .ward("Thach Thang")
                    .street("50 Nguyen Chi Thanh")
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

            ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository).save(addressCaptor.capture());
            Address savedAddress = addressCaptor.getValue();
            assertThat(savedAddress.getRecipientName()).isEqualTo("Van B");
            assertThat(savedAddress.getUser().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("createOrder success with applied voucher")
        void createOrderSuccess_withVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            Address address = Address.builder()
                    .id(addressId)
                    .recipientName("Van A")
                    .phone("0909090909")
                    .province("HCM")
                    .ward("Ben Nghe")
                    .street("1 Le Loi")
                    .user(user)
                    .build();

            when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

            Voucher voucher = Voucher.builder()
                    .id(77L)
                    .code("SAVE100K")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(100000))
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(true)
                    .build();

            when(voucherRepository.findByCode("SAVE100K")).thenReturn(Optional.of(voucher));
            when(voucherRepository.increaseUsedCount(eq(77L), eq(userId))).thenReturn(1);

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(999L);
                return orderToSave;
            });

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .addressId(addressId)
                    .paymentMethod("COD")
                    .voucherCode("SAVE100K")
                    .build();

            OrderResponse response = orderService.createOrder(request);

            assertThat(response.getOrderId()).isEqualTo(999L);
            assertThat(response.getTotalDiscount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(response.getTotalCheckout()).isEqualByComparingTo(BigDecimal.valueOf(2000000 + 30000 - 100000));

            verify(voucherValidator)
                    .validateForCheckout(
                            eq(voucher),
                            eq(user),
                            eq(BigDecimal.valueOf(2000000.00)),
                            eq(BigDecimal.valueOf(2000000.00)));
            verify(voucherRepository).increaseUsedCount(eq(77L), eq(userId));
            verify(voucherRepository).insertVoucherUser(eq(77L), eq(userId));
        }

        @Test
        @DisplayName("createOrder throws VOUCHER_PER_USER_LIMIT_REACHED when validation fails")
        void createOrderThrows_perUserLimitReached() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            Address address = Address.builder()
                    .id(addressId)
                    .recipientName("Van A")
                    .phone("0909090909")
                    .province("HCM")
                    .ward("Ben Nghe")
                    .street("1 Le Loi")
                    .user(user)
                    .build();

            when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

            Voucher voucher = Voucher.builder()
                    .id(77L)
                    .code("SAVE100K")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(100000))
                    .applyScope(VoucherApplyScope.ALL)
                    .maxPerUser(1)
                    .isActive(true)
                    .build();

            when(voucherRepository.findByCode("SAVE100K")).thenReturn(Optional.of(voucher));
            doThrow(new AppException(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED))
                    .when(voucherValidator)
                    .validateForCheckout(any(), any(), any(), any());

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .addressId(addressId)
                    .paymentMethod("COD")
                    .voucherCode("SAVE100K")
                    .build();

            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED.getMessage());
        }

        @Test
        @DisplayName("createOrder throws ADDRESS_REQUIRED when neither addressId nor newAddress is provided")
        void createOrderThrowsAddressRequired() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

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
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            User otherUser = User.builder()
                    .id(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .build();
            Address address = Address.builder().id(addressId).user(otherUser).build();

            when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

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
