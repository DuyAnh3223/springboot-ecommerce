package spring.abtechzone.modules.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.constant.CartMergeItemStatus;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartMergeItemRequest;
import spring.abtechzone.modules.cart.dto.request.CartMergeRequest;
import spring.abtechzone.modules.cart.dto.response.CartMergeResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartMergeLedger;
import spring.abtechzone.modules.cart.mapper.CartItemMapper;
import spring.abtechzone.modules.cart.mapper.CartMapper;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartMergeLedgerRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.cart.service.CartMergeRequestNormalizer;
import spring.abtechzone.modules.cart.service.CartService;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.service.UserService;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartMergeServiceTest {

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

    @Mock
    UserService userService;

    @Mock
    CartMergeLedgerRepository cartMergeLedgerRepository;

    @Mock
    RedissonClient redissonClient;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    TransactionTemplate transactionTemplate;

    @Mock
    RLock lock;

    @Mock
    RBucket<String> bucket;

    CartService cartService;
    User user;
    UUID mergeId;

    @BeforeEach
    void setUp() throws Exception {
        cartService = new CartService(
                cartRepository,
                cartItemRepository,
                productSkuRepository,
                cartItemMapper,
                cartMapper,
                userService,
                cartMergeLedgerRepository,
                redissonClient,
                objectMapper,
                transactionTemplate);
        user = User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .username("testuser")
                .build();
        mergeId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        when(userService.getCurrentUser()).thenReturn(user);
        when(redissonClient.<String>getBucket(anyString())).thenReturn(bucket);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(bucket.get()).thenReturn(null);
        when(lock.tryLock(anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<CartMergeResponse> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @Test
    void mixedBatchMergesValidSkuAndReturnsBusinessRejectionWithoutLosingValidItem() {
        Product product = Product.builder()
                .id(1L)
                .name("Published")
                .published(true)
                .draft(false)
                .build();
        ProductSku validSku = ProductSku.builder()
                .id(17L)
                .price(BigDecimal.valueOf(100))
                .stock(10)
                .active(true)
                .product(product)
                .build();
        ProductSku outOfStockSku = ProductSku.builder()
                .id(42L)
                .price(BigDecimal.valueOf(200))
                .stock(0)
                .active(true)
                .product(product)
                .build();
        when(cartMergeLedgerRepository.findByUserIdAndMergeId(user.getId(), mergeId))
                .thenReturn(Optional.empty());
        when(cartRepository.findByUserIdAndStatusForUpdate(user.getId(), CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(productSkuRepository.findAllWithProductByIdIn(List.of(17L, 42L)))
                .thenReturn(List.of(validSku, outOfStockSku));

        CartMergeResponse response = cartService.mergeGuestCart(request(item(17L, 2), item(42L, 1)));

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getStatus()).isEqualTo(CartMergeItemStatus.MERGED);
        assertThat(response.getItems().get(0).getMergedQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(1).getStatus()).isEqualTo(CartMergeItemStatus.REJECTED);
        assertThat(response.getItems().get(1).getReasonCode()).isEqualTo("INSUFFICIENT_STOCK");
        verify(cartRepository).save(any(Cart.class));
        verify(cartMergeLedgerRepository).save(any(CartMergeLedger.class));
    }

    @Test
    void durableLedgerReplayDoesNotReadOrMutateCartAgain() throws Exception {
        CartMergeResponse stored =
                CartMergeResponse.builder().mergeId(mergeId).items(List.of()).build();
        CartMergeLedger ledger = CartMergeLedger.builder()
                .id(99L)
                .user(user)
                .mergeId(mergeId)
                .requestHash(hashFor(item(17L, 2)))
                .resultJson("stored")
                .build();
        when(cartMergeLedgerRepository.findByUserIdAndMergeId(user.getId(), mergeId))
                .thenReturn(Optional.of(ledger));
        when(objectMapper.readValue(eq("stored"), eq(CartMergeResponse.class))).thenReturn(stored);

        CartMergeResponse response = cartService.mergeGuestCart(request(item(17L, 2)));

        assertThat(response).isSameAs(stored);
        verify(cartRepository, never()).findByUserIdAndStatusForUpdate(any(), any());
        verify(cartMergeLedgerRepository, never()).save(any());
    }

    @Test
    void differentRequestForSameMergeIdReturnsConflictWithoutMutation() {
        CartMergeLedger ledger = CartMergeLedger.builder()
                .id(99L)
                .user(user)
                .mergeId(mergeId)
                .requestHash("different-hash")
                .resultJson("stored")
                .build();
        when(cartMergeLedgerRepository.findByUserIdAndMergeId(user.getId(), mergeId))
                .thenReturn(Optional.of(ledger));

        assertThatThrownBy(() -> cartService.mergeGuestCart(request(item(17L, 2))))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.MERGE_ID_REUSED));
        verify(cartRepository, never()).findByUserIdAndStatusForUpdate(any(), any());
    }

    @Test
    void redisReadFailureReturns503BeforeCartMutation() {
        when(bucket.get()).thenThrow(new IllegalStateException("redis unavailable"));

        assertThatThrownBy(() -> cartService.mergeGuestCart(request(item(17L, 2))))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.SYSTEM_BUSY));
        verifyNoInteractions(cartRepository, cartMergeLedgerRepository, transactionTemplate);
    }

    private CartMergeRequest request(CartMergeItemRequest... items) {
        return CartMergeRequest.builder().mergeId(mergeId).items(List.of(items)).build();
    }

    private CartMergeItemRequest item(long skuId, int quantity) {
        return CartMergeItemRequest.builder().skuId(skuId).quantity(quantity).build();
    }

    private String hashFor(CartMergeItemRequest item) {
        return CartMergeRequestNormalizer.normalize(CartMergeRequest.builder()
                        .mergeId(mergeId)
                        .items(List.of(item))
                        .build())
                .requestHash();
    }
}
