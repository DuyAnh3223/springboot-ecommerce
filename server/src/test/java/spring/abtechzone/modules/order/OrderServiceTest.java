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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.CheckoutChangedException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.constant.PaymentMethod;
import spring.abtechzone.modules.order.dto.request.AddressRequest;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutItemRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedVoucherRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.mapper.OrderMapper;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.order.service.CreateOrderRequestHash;
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
import spring.abtechzone.modules.voucher.repository.VoucherRedemptionRepository;
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
    VoucherRedemptionRepository voucherRedemptionRepository;

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
            lenient().when(mockLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
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

        private CheckoutRequest request(Long... skuIds) {
            return CheckoutRequest.builder().selectedSkuIds(List.of(skuIds)).build();
        }

        @Test
        @DisplayName("checkoutReview success without voucher")
        void reviewSuccess_noVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CheckoutResponse response = orderService.checkoutReview(request(100L));

            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getShippingFee()).isEqualByComparingTo(BigDecimal.valueOf(30000));
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2030000));
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductName()).isEqualTo("iPhone 15 Pro Max");
            assertThat(response.getItems().get(0).getSkuId()).isEqualTo(100L);
            assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
            assertThat(response.getItems().get(0).getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000000.00));
            assertThat(response.getItems().get(0).getLineTotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getItems().get(0).getAvailableStock()).isEqualTo(10);
            assertThat(response.getItems().get(0).getIssueCode()).isNull();
            assertThat(response.getEligibleSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getVoucher()).isNull();
            assertThat(response.isCanPlaceOrder()).isTrue();

            verify(voucherValidator, never()).validateForCheckout(any(), any(), any(), any());
        }

        @Test
        @DisplayName("checkoutReview normalizes selection: deduplicates and sorts ascending")
        void reviewSuccess_normalizesSelection() {
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
            when(productSkuRepository.findById(200L)).thenReturn(Optional.of(sku2));

            // Unsorted + duplicate selection
            CheckoutResponse response = orderService.checkoutReview(request(200L, 100L, 200L));

            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getItems().get(0).getSkuId()).isEqualTo(100L);
            assertThat(response.getItems().get(1).getSkuId()).isEqualTo(200L);
            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2500000.00));
        }

        @Test
        @DisplayName("checkoutReview ignores unselected cart items entirely")
        void reviewSuccess_ignoresUnselectedItems() {
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

            CheckoutResponse response = orderService.checkoutReview(request(100L));

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getSkuId()).isEqualTo(100L);
            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2030000));
        }

        @Test
        @DisplayName("checkoutReview rejects selected SKU not in the active cart (owner-safe)")
        void reviewThrows_skuNotInCart() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CheckoutRequest request =
                    CheckoutRequest.builder().selectedSkuIds(List.of(999L)).build();

            assertThatThrownBy(() -> orderService.checkoutReview(request))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.CART_ITEM_NOT_IN_CART.getMessage());
        }

        @Test
        @DisplayName("checkoutReview rejects null/empty/non-positive selection")
        void reviewThrows_invalidSelection() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> orderService.checkoutReview(
                            CheckoutRequest.builder().selectedSkuIds(null).build()))
                    .isInstanceOf(AppException.class);

            assertThatThrownBy(() -> orderService.checkoutReview(
                            CheckoutRequest.builder().selectedSkuIds(List.of()).build()))
                    .isInstanceOf(AppException.class);

            assertThatThrownBy(() -> orderService.checkoutReview(CheckoutRequest.builder()
                            .selectedSkuIds(List.of(0L, -1L))
                            .build()))
                    .isInstanceOf(AppException.class);
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

            CheckoutRequest request = CheckoutRequest.builder()
                    .selectedSkuIds(List.of(100L))
                    .voucherCode("SALE10")
                    .build();
            CheckoutResponse response = orderService.checkoutReview(request);

            assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(200000.00));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000000 + 30000 - 200000));
            assertThat(response.getVoucher().getCode()).isEqualTo("SALE10");
            assertThat(response.getVoucher().isApplicable()).isTrue();
            assertThat(response.getVoucher().getIssueCode()).isNull();
            assertThat(response.isCanPlaceOrder()).isTrue();

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
            when(productSkuRepository.findById(200L)).thenReturn(Optional.of(sku2));

            Voucher voucher = Voucher.builder()
                    .code("SPECIFIC10")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10.0))
                    .isActive(true)
                    .applyScope(VoucherApplyScope.SPECIFIC)
                    .productSkus(Set.of(sku))
                    .build();

            when(voucherRepository.findByCode("SPECIFIC10")).thenReturn(Optional.of(voucher));

            CheckoutRequest request = CheckoutRequest.builder()
                    .selectedSkuIds(List.of(100L, 200L))
                    .voucherCode("SPECIFIC10")
                    .build();
            CheckoutResponse response = orderService.checkoutReview(request);

            // Subtotal = 2.000.000 (sku 100) + 500.000 (sku 200) = 2.500.000
            // Discount = 10% of 2.000.000 (eligible SKU only) = 200.000
            assertThat(response.getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2500000.00));
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(200000.00));
            assertThat(response.getEligibleSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000.00));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2500000 + 30000 - 200000));

            verify(voucherValidator)
                    .validateForCheckout(
                            eq(voucher),
                            eq(user),
                            eq(BigDecimal.valueOf(2500000.00)),
                            eq(BigDecimal.valueOf(2000000.00)));
        }

        @Test
        @DisplayName("checkoutReview invalid voucher returns typed review with canPlaceOrder=false")
        void review_invalidVoucherIsTypedReview() {
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

            CheckoutRequest request = CheckoutRequest.builder()
                    .selectedSkuIds(List.of(100L))
                    .voucherCode("ONCE_ONLY")
                    .build();
            CheckoutResponse response = orderService.checkoutReview(request);

            assertThat(response.getVoucher()).isNotNull();
            assertThat(response.getVoucher().getCode()).isEqualTo("ONCE_ONLY");
            assertThat(response.getVoucher().isApplicable()).isFalse();
            assertThat(response.getVoucher().getIssueCode()).isEqualTo(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED.name());
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.isCanPlaceOrder()).isFalse();
        }

        @Test
        @DisplayName("checkoutReview returns typed issue for insufficient stock with canPlaceOrder=false")
        void review_insufficientStockIsTypedReview() {
            cartItem.setQuantity(15); // stock is only 10
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CheckoutResponse response = orderService.checkoutReview(request(100L));

            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getIssueCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK.name());
            assertThat(response.isCanPlaceOrder()).isFalse();
            // Contract R-C03-04: totalAmount = max(0, subtotal + shippingFee - discountAmount),
            // regardless of canPlaceOrder — the breakdown must stay internally consistent.
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000000 + 30000));
        }

        @Test
        @DisplayName("checkoutReview snapshot contains no fingerprint/token/expiry")
        void reviewSnapshot_hasNoFingerprintOrToken() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CheckoutResponse response = orderService.checkoutReview(request(100L));

            assertThat(response).hasNoNullFieldsOrPropertiesExcept("voucher", "subtotal");
            // Sanity: the reviewed snapshot exposes only order-affecting + display fields
            assertThat(response.getItems().get(0)).hasNoNullFieldsOrPropertiesExcept("issueCode", "imageUrl");
        }
    }

    @Nested
    @DisplayName("createOrder request hash")
    class RequestHashTests {

        private CreateOrderRequest requestWithAddress(String recipientName, String phone, boolean saveAddress) {
            return CreateOrderRequest.builder()
                    .reviewedCheckout(ReviewedCheckoutRequest.builder()
                            .items(List.of(ReviewedCheckoutItemRequest.builder()
                                    .skuId(100L)
                                    .quantity(2)
                                    .unitPrice(BigDecimal.valueOf(1000000.00))
                                    .lineTotal(BigDecimal.valueOf(2000000.00))
                                    .build()))
                            .subtotal(BigDecimal.valueOf(2000000.00))
                            .eligibleSubtotal(BigDecimal.valueOf(2000000.00))
                            .shippingFee(BigDecimal.valueOf(30000))
                            .discountAmount(BigDecimal.ZERO)
                            .totalAmount(BigDecimal.valueOf(2030000))
                            .canPlaceOrder(true)
                            .build())
                    .newUserAddress(AddressRequest.builder()
                            .recipientName(recipientName)
                            .phone(phone)
                            .province("Da Nang")
                            .ward("Thuan Phuoc")
                            .street("100 Le Loi")
                            .saveAddress(saveAddress)
                            .build())
                    .paymentMethod(PaymentMethod.COD)
                    .build();
        }

        @Test
        @DisplayName("Address field boundaries are unambiguous: A|B/C and A/B|C hash differently")
        void addressDelimiter_doesNotCollide() {
            // recipientName="A|B", phone="C" vs recipientName="A", phone="B|C"
            // must never produce the same canonical string.
            String h1 = CreateOrderRequestHash.compute(requestWithAddress("A|B", "C", false), userId);
            String h2 = CreateOrderRequestHash.compute(requestWithAddress("A", "B|C", false), userId);
            assertThat(h1).isNotEqualTo(h2);
        }

        @Test
        @DisplayName("saveAddress flag is part of the hash (side-effect field)")
        void saveAddress_changesHash() {
            String h1 = CreateOrderRequestHash.compute(requestWithAddress("Van B", "0808080808", false), userId);
            String h2 = CreateOrderRequestHash.compute(requestWithAddress("Van B", "0808080808", true), userId);
            assertThat(h1).isNotEqualTo(h2);
        }

        @Test
        @DisplayName("Hash is stable across item order and equivalent BigDecimal scale")
        void hash_isOrderAndScaleIndependent() {
            ReviewedCheckoutRequest reviewed = ReviewedCheckoutRequest.builder()
                    .items(List.of(
                            ReviewedCheckoutItemRequest.builder()
                                    .skuId(200L)
                                    .quantity(1)
                                    .unitPrice(BigDecimal.valueOf(500000.00))
                                    .lineTotal(BigDecimal.valueOf(500000.00))
                                    .build(),
                            ReviewedCheckoutItemRequest.builder()
                                    .skuId(100L)
                                    .quantity(2)
                                    .unitPrice(new BigDecimal("1000000"))
                                    .lineTotal(new BigDecimal("2000000"))
                                    .build()))
                    .subtotal(new BigDecimal("2500000"))
                    .eligibleSubtotal(new BigDecimal("2500000.00"))
                    .shippingFee(BigDecimal.valueOf(30000))
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.valueOf(2530000))
                    .canPlaceOrder(true)
                    .build();

            CreateOrderRequest a = CreateOrderRequest.builder()
                    .reviewedCheckout(reviewed)
                    .newUserAddress(AddressRequest.builder()
                            .recipientName("Van A")
                            .phone("0909090909")
                            .province("HCM")
                            .ward("Ben Nghe")
                            .street("1 Le Loi")
                            .saveAddress(false)
                            .build())
                    .paymentMethod(PaymentMethod.COD)
                    .build();

            // Same content, different item order (reversed), different scale
            ReviewedCheckoutRequest reversed = ReviewedCheckoutRequest.builder()
                    .items(List.of(
                            ReviewedCheckoutItemRequest.builder()
                                    .skuId(100L)
                                    .quantity(2)
                                    .unitPrice(BigDecimal.valueOf(1000000.00))
                                    .lineTotal(BigDecimal.valueOf(2000000.00))
                                    .build(),
                            ReviewedCheckoutItemRequest.builder()
                                    .skuId(200L)
                                    .quantity(1)
                                    .unitPrice(BigDecimal.valueOf(500000.00))
                                    .lineTotal(BigDecimal.valueOf(500000.00))
                                    .build()))
                    .subtotal(new BigDecimal("2500000.00"))
                    .eligibleSubtotal(new BigDecimal("2500000"))
                    .shippingFee(BigDecimal.valueOf(30000))
                    .discountAmount(new BigDecimal("0.00"))
                    .totalAmount(new BigDecimal("2530000.00"))
                    .canPlaceOrder(true)
                    .build();
            CreateOrderRequest b = CreateOrderRequest.builder()
                    .reviewedCheckout(reversed)
                    .newUserAddress(AddressRequest.builder()
                            .recipientName("Van A")
                            .phone("0909090909")
                            .province("HCM")
                            .ward("Ben Nghe")
                            .street("1 Le Loi")
                            .saveAddress(false)
                            .build())
                    .paymentMethod(PaymentMethod.COD)
                    .build();

            assertThat(CreateOrderRequestHash.compute(a, userId)).isEqualTo(CreateOrderRequestHash.compute(b, userId));
        }

        @Test
        @DisplayName("Hash differs when any reviewed monetary or voucher value differs")
        void hash_differsOnAnyMonetaryChange() {
            String base = CreateOrderRequestHash.compute(requestWithAddress("Van B", "0808080808", false), userId);

            CreateOrderRequest changed = requestWithAddress("Van B", "0808080808", false);
            changed.getReviewedCheckout().setTotalAmount(BigDecimal.valueOf(2530000));
            assertThat(CreateOrderRequestHash.compute(changed, userId)).isNotEqualTo(base);

            CreateOrderRequest voucher = requestWithAddress("Van B", "0808080808", false);
            voucher.getReviewedCheckout()
                    .setVoucher(ReviewedVoucherRequest.builder()
                            .code("SUMMER")
                            .applicable(true)
                            .build());
            assertThat(CreateOrderRequestHash.compute(voucher, userId)).isNotEqualTo(base);
        }
    }

    @Nested
    @DisplayName("createOrder tests")
    class CreateOrderTests {

        private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";

        private ReviewedCheckoutRequest reviewedCheckout() {
            return ReviewedCheckoutRequest.builder()
                    .items(List.of(ReviewedCheckoutItemRequest.builder()
                            .skuId(100L)
                            .quantity(2)
                            .unitPrice(BigDecimal.valueOf(1000000.00))
                            .lineTotal(BigDecimal.valueOf(2000000.00))
                            .build()))
                    .subtotal(BigDecimal.valueOf(2000000.00))
                    .eligibleSubtotal(BigDecimal.valueOf(2000000.00))
                    .shippingFee(BigDecimal.valueOf(30000))
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.valueOf(2030000))
                    .canPlaceOrder(true)
                    .build();
        }

        private ReviewedCheckoutRequest reviewedCheckoutWithVoucher(Voucher voucher) {
            return ReviewedCheckoutRequest.builder()
                    .items(List.of(ReviewedCheckoutItemRequest.builder()
                            .skuId(100L)
                            .quantity(2)
                            .unitPrice(BigDecimal.valueOf(1000000.00))
                            .lineTotal(BigDecimal.valueOf(2000000.00))
                            .build()))
                    .subtotal(BigDecimal.valueOf(2000000.00))
                    .eligibleSubtotal(BigDecimal.valueOf(2000000.00))
                    .shippingFee(BigDecimal.valueOf(30000))
                    .discountAmount(voucherService.getDiscount(voucher, BigDecimal.valueOf(2000000.00)))
                    .totalAmount(BigDecimal.valueOf(2000000 + 30000)
                            .subtract(voucherService.getDiscount(voucher, BigDecimal.valueOf(2000000.00))))
                    .voucher(ReviewedVoucherRequest.builder()
                            .code(voucher.getCode())
                            .applicable(true)
                            .build())
                    .canPlaceOrder(true)
                    .build();
        }

        private CreateOrderRequest requestWithSavedAddress(ReviewedCheckoutRequest reviewed) {
            return CreateOrderRequest.builder()
                    .reviewedCheckout(reviewed)
                    .addressId(addressId)
                    .paymentMethod(PaymentMethod.COD)
                    .build();
        }

        @Test
        @DisplayName("createOrder success with Saved Address ID")
        void createOrderSuccess_savedAddress() throws Exception {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
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

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());

            OrderResponse response = orderService.createOrder(request, IDEMPOTENCY_KEY);

            assertThat(response.getId()).isEqualTo(999L);
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2030000));

            assertThat(sku.getStock()).isEqualTo(8);

            assertThat(cart.getStatus()).isEqualTo(CartStatus.COMPLETED);

            verify(orderRepository).save(any(Order.class));
            verify(inventoryService).reserveStock(eq(sku), eq(2), any());

            // Watchdog overload used, no fixed lease (AC-C04-05)
            verify(redissonClient.getLock(anyString()), atLeastOnce()).tryLock(anyLong(), any(TimeUnit.class));
            verify(redissonClient.getLock(anyString()), never()).tryLock(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("createOrder releases acquired locks when a later lock times out")
        void createOrder_partialLockFailure_releasesPreviouslyAcquiredLocks() throws Exception {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());

            RLock firstLock = mock(RLock.class);
            RLock secondLock = mock(RLock.class);
            when(redissonClient.getLock(anyString())).thenReturn(firstLock, secondLock);
            when(firstLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(secondLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

            assertThatThrownBy(() ->
                            orderService.createOrder(requestWithSavedAddress(reviewedCheckout()), IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SYSTEM_BUSY);

            verify(firstLock).unlock();
            verify(secondLock, never()).unlock();
            verify(transactionTemplate, never()).execute(any());
        }

        @Test
        @DisplayName("createOrder releases all locks in reverse order when the transaction fails")
        void createOrder_transactionFailure_releasesAllLocksInReverseOrder() throws Exception {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());

            RLock firstLock = mock(RLock.class);
            RLock secondLock = mock(RLock.class);
            when(redissonClient.getLock(anyString())).thenReturn(firstLock, secondLock);
            when(firstLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            when(secondLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
            doThrow(new AppException(ErrorCode.SYSTEM_ERROR))
                    .when(transactionTemplate)
                    .execute(any());

            assertThatThrownBy(() ->
                            orderService.createOrder(requestWithSavedAddress(reviewedCheckout()), IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SYSTEM_ERROR);

            InOrder releaseOrder = inOrder(secondLock, firstLock);
            releaseOrder.verify(secondLock).unlock();
            releaseOrder.verify(firstLock).unlock();
        }

        @Test
        @DisplayName("createOrder stops after the configured idempotency retry limit")
        void createOrder_idempotencyConflictStopsAfterRetryLimit() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            doThrow(new DataIntegrityViolationException("duplicate idempotency key"))
                    .when(transactionTemplate)
                    .execute(any());

            assertThatThrownBy(() ->
                            orderService.createOrder(requestWithSavedAddress(reviewedCheckout()), IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SYSTEM_ERROR);

            verify(transactionTemplate, times(3)).execute(any());
        }

        @Test
        @DisplayName("createOrder does not load the cart before the transaction")
        void createOrder_doesNotLoadCartBeforeTransaction() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(999L);
                return orderToSave;
            });

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

            // A different cart is returned the second time; the authoritative cart for the
            // semantic comparison and mutations must be the in-transaction load (freshCart),
            // never the pre-lock read.
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart), Optional.of(cart));

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());

            orderService.createOrder(request, IDEMPOTENCY_KEY);

            // No cart read may happen before the transaction starts: with OSIV enabled a
            // pre-lock load would seed the persistence context and could serve a stale cart
            // inside the transaction. Every load must occur after locks are acquired.
            InOrder inOrder = inOrder(redissonClient, transactionTemplate, cartRepository);
            inOrder.verify(redissonClient, atLeastOnce()).getLock(anyString());
            inOrder.verify(transactionTemplate).execute(any());
            inOrder.verify(cartRepository, atLeastOnce()).findByUserIdAndStatus(any(), any());
        }

        @Test
        @DisplayName("createOrder success with New Address and saveAddress = true")
        void createOrderSuccess_newAddressAndSave() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
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
                    .reviewedCheckout(reviewedCheckout())
                    .newUserAddress(addressReq)
                    .paymentMethod(PaymentMethod.COD)
                    .build();

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(888L);
                return orderToSave;
            });

            OrderResponse response = orderService.createOrder(request, IDEMPOTENCY_KEY);

            assertThat(response.getId()).isEqualTo(888L);

            ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository).save(addressCaptor.capture());
            Address savedAddress = addressCaptor.getValue();
            assertThat(savedAddress.getRecipientName()).isEqualTo("Van B");
            assertThat(savedAddress.getUser().getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("createOrder success with applied voucher and REDEEMED redemption")
        void createOrderSuccess_withVoucher() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
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

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckoutWithVoucher(voucher));

            OrderResponse response = orderService.createOrder(request, IDEMPOTENCY_KEY);

            assertThat(response.getId()).isEqualTo(999L);
            assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000000 + 30000 - 100000));

            verify(voucherValidator)
                    .validateForCheckout(
                            eq(voucher),
                            eq(user),
                            eq(BigDecimal.valueOf(2000000.00)),
                            eq(BigDecimal.valueOf(2000000.00)));
            verify(voucherRepository).increaseUsedCount(eq(77L), eq(userId));
            verify(voucherRedemptionRepository).save(any());
        }

        @Test
        @DisplayName("createOrder replay: same key + same hash returns the existing order before cart lookup")
        void createOrder_replaySameKeyAndHash() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());
            String requestHash = CreateOrderRequestHash.compute(request, userId);

            Order existing = Order.builder()
                    .id(555L)
                    .orderCode("ORD-REPLAY")
                    .status(OrderStatus.PENDING)
                    .requestHash(requestHash)
                    .build();
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.of(existing));

            OrderResponse response = orderService.createOrder(request, IDEMPOTENCY_KEY);

            assertThat(response.getId()).isEqualTo(555L);
            // Replay must happen BEFORE the cart lookup (AC-C04-01/02)
            verify(cartRepository, never()).findByUserIdAndStatus(any(), any());
            verify(orderRepository, never()).save(any());
            verify(inventoryService, never()).reserveStock(any(), anyInt(), any());
        }

        @Test
        @DisplayName("createOrder same key + different request hash returns 409 IDEMPOTENCY_KEY_REUSED")
        void createOrder_replaySameKeyDifferentHash() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());
            Order existing = Order.builder()
                    .id(555L)
                    .orderCode("ORD-REPLAY")
                    .status(OrderStatus.PENDING)
                    .requestHash("another-hash")
                    .build();
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> orderService.createOrder(request, IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.IDEMPOTENCY_KEY_REUSED.getMessage());
        }

        @Test
        @DisplayName("createOrder throws ADDRESS_REQUIRED when both addressId and newAddress are missing (XOR)")
        void createOrderThrowsAddressRequired() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .reviewedCheckout(reviewedCheckout())
                    .paymentMethod(PaymentMethod.COD)
                    .build();

            assertThatThrownBy(() -> orderService.createOrder(request, IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ADDRESS_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("createOrder throws ADDRESS_REQUIRED when both addressId and newAddress are provided (XOR)")
        void createOrderThrowsAddressBothProvided() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .reviewedCheckout(reviewedCheckout())
                    .addressId(addressId)
                    .newUserAddress(AddressRequest.builder()
                            .recipientName("Van B")
                            .phone("0808080808")
                            .province("Da Nang")
                            .ward("Thach Thang")
                            .street("50 Nguyen Chi Thanh")
                            .build())
                    .paymentMethod(PaymentMethod.COD)
                    .build();

            assertThatThrownBy(() -> orderService.createOrder(request, IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ADDRESS_REQUIRED.getMessage());
        }

        @Test
        @DisplayName("createOrder throws ADDRESS_NOT_BELONG_TO_USER when user attempts to use other user's address")
        void createOrderThrowsAddressNotBelongToUser() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            User otherUser = User.builder()
                    .id(java.util.UUID.fromString("22222222-2222-2222-2222-222222222222"))
                    .build();
            Address address = Address.builder().id(addressId).user(otherUser).build();

            when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());

            assertThatThrownBy(() -> orderService.createOrder(request, IDEMPOTENCY_KEY))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.ADDRESS_NOT_BELONG_TO_USER.getMessage());
        }

        @Test
        @DisplayName("createOrder unit price increase returns 409 CHECKOUT_CHANGED with latest review, no mutation")
        void createOrder_priceIncreaseMismatch() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            // Reviewed snapshot still carries the old price; authoritative DB price has increased.
            ProductSku changedSku = ProductSku.builder()
                    .id(100L)
                    .sku("IPHONE-15-256GB")
                    .price(BigDecimal.valueOf(1100000.00))
                    .stock(10)
                    .product(user != null ? sku.getProduct() : null)
                    .build();
            when(productSkuRepository.findById(100L)).thenReturn(Optional.of(changedSku));

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());

            assertThatThrownBy(() -> orderService.createOrder(request, IDEMPOTENCY_KEY))
                    .isInstanceOf(CheckoutChangedException.class);
            assertThat(sku.getStock()).isEqualTo(10);
            verify(orderRepository, never()).save(any());
            verify(inventoryService, never()).reserveStock(any(), anyInt(), any());
        }

        @Test
        @DisplayName("createOrder raw stock change while sufficient does NOT false-mismatch")
        void createOrder_rawStockChangeStillSufficient() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
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

            // Stock changed 10 -> 7 but still sufficient for quantity 2
            ProductSku changedStockSku = ProductSku.builder()
                    .id(100L)
                    .sku("IPHONE-15-256GB")
                    .price(BigDecimal.valueOf(1000000.00))
                    .stock(7)
                    .product(sku.getProduct())
                    .build();
            when(productSkuRepository.findById(100L)).thenReturn(Optional.of(changedStockSku));

            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());

            OrderResponse response = orderService.createOrder(request, IDEMPOTENCY_KEY);

            assertThat(response.getId()).isEqualTo(999L);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("createOrder reviewed monetary fields are never the persistence source")
        void createOrder_clientTamperedPricesNotPersisted() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());
            when(cartRepository.findByUserIdAndStatus(any(), any())).thenReturn(Optional.of(cart));

            // Client tampered: unitPrice 900000 and total 2030000 differ from authoritative review -> 409, no order
            ReviewedCheckoutRequest tampered = ReviewedCheckoutRequest.builder()
                    .items(List.of(ReviewedCheckoutItemRequest.builder()
                            .skuId(100L)
                            .quantity(2)
                            .unitPrice(BigDecimal.valueOf(900000.00))
                            .lineTotal(BigDecimal.valueOf(1800000.00))
                            .build()))
                    .subtotal(BigDecimal.valueOf(1800000.00))
                    .eligibleSubtotal(BigDecimal.valueOf(1800000.00))
                    .shippingFee(BigDecimal.valueOf(30000))
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.valueOf(2030000))
                    .canPlaceOrder(true)
                    .build();
            CreateOrderRequest request = requestWithSavedAddress(tampered);

            assertThatThrownBy(() -> orderService.createOrder(request, IDEMPOTENCY_KEY))
                    .isInstanceOf(CheckoutChangedException.class);
            verify(orderRepository, never()).save(any());
            verify(inventoryService, never()).reserveStock(any(), anyInt(), any());
        }

        @Test
        @DisplayName("createOrder partial selection only processes selected lines")
        void createOrder_partialSelection() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(orderRepository.findByUserIdAndIdempotencyKey(eq(userId), eq(IDEMPOTENCY_KEY)))
                    .thenReturn(Optional.empty());

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

            // Only SKU 100 selected; SKU 200 stays in the cart
            CreateOrderRequest request = requestWithSavedAddress(reviewedCheckout());

            OrderResponse response = orderService.createOrder(request, IDEMPOTENCY_KEY);

            assertThat(response.getId()).isEqualTo(999L);
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().get(0).getProductSku().getId()).isEqualTo(200L);
            assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
            verify(inventoryService).reserveStock(eq(sku), eq(2), any());
            verify(inventoryService, never()).reserveStock(eq(sku2), anyInt(), any());
        }
    }
}
