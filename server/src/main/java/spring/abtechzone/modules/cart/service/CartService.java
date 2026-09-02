package spring.abtechzone.modules.cart.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.constant.CartMergeItemStatus;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.request.CartMergeRequest;
import spring.abtechzone.modules.cart.dto.request.UpdateQuantityRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartMergeItemResponse;
import spring.abtechzone.modules.cart.dto.response.CartMergeResponse;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.entity.CartMergeLedger;
import spring.abtechzone.modules.cart.mapper.CartItemMapper;
import spring.abtechzone.modules.cart.mapper.CartMapper;
import spring.abtechzone.modules.cart.repository.CartItemRepository;
import spring.abtechzone.modules.cart.repository.CartMergeLedgerRepository;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.service.UserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartService {

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductSkuRepository productSkuRepository;
    CartItemMapper cartItemMapper;
    CartMapper cartMapper;
    UserService userService;
    CartMergeLedgerRepository cartMergeLedgerRepository;
    RedissonClient redissonClient;
    ObjectMapper objectMapper;
    TransactionTemplate transactionTemplate;
    InventoryService inventoryService;

    private static final long MERGE_LOCK_WAIT_SECONDS = 5;
    private static final Duration MERGE_CACHE_TTL = Duration.ofHours(24);

    // ────────────────────────────────────────────────────────
    // POST /cart/add
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        if (request == null || request.getQuantity() == null || request.getQuantity() < 1) {
            throw new AppException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
        }

        if (request.getProductSkuId() == null) {
            throw new AppException(ErrorCode.SKU_NOT_FOUND);
        }

        // Find ProductSku
        ProductSku productSku = productSkuRepository
                .findById(request.getProductSkuId())
                .orElseThrow(() -> new AppException(ErrorCode.SKU_NOT_FOUND));

        User user = userService.getCurrentUser();

        int stock = inventoryService.getOnHandOrZero(productSku.getId());

        // Validation 1: Request quantity must be <= stock
        if (request.getQuantity() > stock) {
            throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
        }

        // Validation 2: Cumulative quantity must be <= stock
        Optional<Cart> existingCartOpt = cartRepository.findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE);
        Optional<CartItem> existingItem = Optional.empty();
        if (existingCartOpt.isPresent()) {
            existingItem = existingCartOpt.get().getItems().stream()
                    .filter(item -> item.getProductSku().getId().equals(productSku.getId()))
                    .findFirst();
        }

        if (existingItem.isPresent()) {
            long newQuantity = (long) existingItem.get().getQuantity() + request.getQuantity();
            if (newQuantity > stock) {
                throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
            }
        }

        // Both validation pass  → get/create Cart and Save Item
        Cart cart = existingCartOpt.orElseGet(() -> {
            Cart newCart = Cart.builder()
                    .user(user)
                    .status(CartStatus.ACTIVE)
                    .items(new ArrayList<>())
                    .build();
            return cartRepository.save(newCart);
        });

        // Re-check item in cart
        Optional<CartItem> itemInCartOpt = cart.getItems().stream()
                .filter(item -> item.getProductSku().getId().equals(productSku.getId()))
                .findFirst();

        if (itemInCartOpt.isPresent()) {
            CartItem item = itemInCartOpt.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setUnitPrice(productSku.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productSku(productSku)
                    .quantity(request.getQuantity())
                    .unitPrice(productSku.getPrice())
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return cartMapper.toCartResponse(cart);
    }

    /**
     * Merge one authenticated guest-cart attempt. Redis is only a fast path;
     * the ledger written with the cart transaction is the durable replay guard.
     */
    public CartMergeResponse mergeGuestCart(CartMergeRequest request) {
        CartMergeRequestNormalizer.NormalizedCartMerge normalized = CartMergeRequestNormalizer.normalize(request);
        User user = userService.getCurrentUser();
        String cacheKey = mergeCacheKey(user.getId(), normalized.mergeId());

        CartMergeResponse cached = readCached(cacheKey, normalized);
        if (cached != null) {
            return cached;
        }

        RLock lock;
        try {
            lock = redissonClient.getLock("lock:user-cart:" + user.getId());
            if (!lock.tryLock(MERGE_LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AppException(ErrorCode.SYSTEM_BUSY);
            }
        } catch (AppException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_BUSY);
        } catch (RuntimeException exception) {
            log.warn("Cannot acquire guest-cart merge lock for user {}", user.getId(), exception);
            throw new AppException(ErrorCode.SYSTEM_BUSY);
        }

        try {
            CartMergeResponse lockedCached = readCached(cacheKey, normalized);
            if (lockedCached != null) {
                return lockedCached;
            }

            CartMergeResponse result;
            try {
                result = transactionTemplate.execute(status -> mergeInTransaction(user, normalized));
            } catch (AppException exception) {
                throw exception;
            } catch (DataAccessException | IllegalStateException exception) {
                log.error(
                        "Guest-cart merge transaction failed for user {} and merge {}",
                        user.getId(),
                        normalized.mergeId(),
                        exception);
                throw new AppException(ErrorCode.SYSTEM_ERROR);
            }
            if (result == null) {
                throw new AppException(ErrorCode.SYSTEM_ERROR);
            }

            writeCachedSafely(cacheKey, normalized.requestHash(), result);
            return result;
        } finally {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (RuntimeException exception) {
                log.warn("Cannot release guest-cart merge lock for user {}", user.getId(), exception);
            }
        }
    }

    private CartMergeResponse mergeInTransaction(User user, CartMergeRequestNormalizer.NormalizedCartMerge normalized) {
        CartMergeLedger existingLedger = cartMergeLedgerRepository
                .findByUserIdAndMergeId(user.getId(), normalized.mergeId())
                .orElse(null);
        if (existingLedger != null) {
            return replayLedger(existingLedger, normalized.requestHash());
        }

        Cart cart = cartRepository
                .findByUserIdAndStatusForUpdate(user.getId(), CartStatus.ACTIVE)
                .orElse(null);
        List<Long> skuIds = normalized.items().stream()
                .map(CartMergeRequestNormalizer.NormalizedCartMergeItem::skuId)
                .toList();
        Map<Long, ProductSku> skuById = new HashMap<>();
        productSkuRepository.findAllWithProductByIdIn(skuIds).forEach(sku -> skuById.put(sku.getId(), sku));
        Map<Long, Integer> onHandBySkuId = inventoryService.getOnHandBySkuIds(skuIds);
        Map<Long, CartItem> existingItems = new HashMap<>();
        if (cart != null && cart.getItems() != null) {
            cart.getItems().forEach(item -> {
                if (item.getProductSku() != null && item.getProductSku().getId() != null) {
                    existingItems.put(item.getProductSku().getId(), item);
                }
            });
        }

        List<CartMergeItemResponse> results = new ArrayList<>();
        List<CartMergeRequestNormalizer.NormalizedCartMergeItem> acceptedItems = new ArrayList<>();
        Map<Long, Integer> finalQuantities = new HashMap<>();
        for (CartMergeRequestNormalizer.NormalizedCartMergeItem item : normalized.items()) {
            ProductSku sku = skuById.get(item.skuId());
            String rejection = mergeRejectionReason(
                    sku, existingItems.get(item.skuId()), item.quantity(), onHandBySkuId.getOrDefault(item.skuId(), 0));
            if (rejection != null) {
                results.add(rejectedResult(item, rejection));
                continue;
            }

            int existingQuantity = existingItems.containsKey(item.skuId())
                    ? existingItems.get(item.skuId()).getQuantity()
                    : 0;
            int finalQuantity = existingQuantity + item.quantity();
            acceptedItems.add(item);
            finalQuantities.put(item.skuId(), finalQuantity);
            results.add(CartMergeItemResponse.builder()
                    .skuId(item.skuId())
                    .requestedQuantity(item.quantity())
                    .mergedQuantity(finalQuantity)
                    .status(CartMergeItemStatus.MERGED)
                    .build());
        }

        if (!acceptedItems.isEmpty()) {
            if (cart == null) {
                cart = Cart.builder()
                        .user(user)
                        .status(CartStatus.ACTIVE)
                        .items(new ArrayList<>())
                        .build();
            } else if (cart.getItems() == null) {
                cart.setItems(new ArrayList<>());
            }
            for (CartMergeRequestNormalizer.NormalizedCartMergeItem item : acceptedItems) {
                CartItem existingItem = existingItems.get(item.skuId());
                ProductSku sku = skuById.get(item.skuId());
                if (existingItem != null) {
                    existingItem.setQuantity(finalQuantities.get(item.skuId()));
                    existingItem.setUnitPrice(sku.getPrice());
                } else {
                    cart.getItems()
                            .add(CartItem.builder()
                                    .cart(cart)
                                    .productSku(sku)
                                    .quantity(finalQuantities.get(item.skuId()))
                                    .unitPrice(sku.getPrice())
                                    .build());
                }
            }
            cartRepository.save(cart);
        }

        CartMergeResponse response = CartMergeResponse.builder()
                .mergeId(normalized.mergeId())
                .items(results)
                .build();
        cartMergeLedgerRepository.save(CartMergeLedger.builder()
                .user(user)
                .mergeId(normalized.mergeId())
                .requestHash(normalized.requestHash())
                .resultJson(toJson(response))
                .build());
        return response;
    }

    private String mergeRejectionReason(ProductSku sku, CartItem existingItem, int requestedQuantity, int onHand) {
        if (sku == null) {
            return "SKU_NOT_FOUND";
        }
        if (!sku.isActive()) {
            return "SKU_INACTIVE";
        }
        if (sku.getProduct() == null
                || !sku.getProduct().isPublished()
                || sku.getProduct().isDraft()) {
            return "PRODUCT_NOT_SELLABLE";
        }
        int existingQuantity =
                existingItem == null || existingItem.getQuantity() == null ? 0 : existingItem.getQuantity();
        if (existingQuantity < 0 || requestedQuantity > Integer.MAX_VALUE - existingQuantity) {
            return "QUANTITY_OVERFLOW";
        }
        if (existingQuantity + requestedQuantity > onHand) {
            return "INSUFFICIENT_STOCK";
        }
        return null;
    }

    private CartMergeItemResponse rejectedResult(
            CartMergeRequestNormalizer.NormalizedCartMergeItem item, String reason) {
        return CartMergeItemResponse.builder()
                .skuId(item.skuId())
                .requestedQuantity(item.quantity())
                .mergedQuantity(0)
                .status(CartMergeItemStatus.REJECTED)
                .reasonCode(reason)
                .build();
    }

    private CartMergeResponse readCached(String cacheKey, CartMergeRequestNormalizer.NormalizedCartMerge normalized) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(cacheKey);
            String raw = bucket.get();
            if (raw == null) {
                return null;
            }
            CachedMergeValue cached = objectMapper.readValue(raw, CachedMergeValue.class);
            if (!normalized.requestHash().equals(cached.requestHash())) {
                throw new AppException(ErrorCode.MERGE_ID_REUSED);
            }
            return cached.response();
        } catch (AppException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Cannot read guest-cart merge cache {}", cacheKey, exception);
            throw new AppException(ErrorCode.SYSTEM_BUSY);
        }
    }

    private void writeCachedSafely(String cacheKey, String requestHash, CartMergeResponse response) {
        try {
            String raw = objectMapper.writeValueAsString(new CachedMergeValue(requestHash, response));
            redissonClient.<String>getBucket(cacheKey).set(raw, MERGE_CACHE_TTL);
        } catch (RuntimeException exception) {
            log.warn("Cannot write guest-cart merge cache {} after commit", cacheKey, exception);
        }
    }

    private CartMergeResponse replayLedger(CartMergeLedger ledger, String requestHash) {
        if (!requestHash.equals(ledger.getRequestHash())) {
            throw new AppException(ErrorCode.MERGE_ID_REUSED);
        }
        try {
            return objectMapper.readValue(ledger.getResultJson(), CartMergeResponse.class);
        } catch (JacksonException exception) {
            log.error("Guest-cart merge ledger {} contains invalid JSON", ledger.getId(), exception);
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String toJson(CartMergeResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String mergeCacheKey(UUID userId, UUID mergeId) {
        return "cart-merge:" + userId + ":" + mergeId;
    }

    private record CachedMergeValue(String requestHash, CartMergeResponse response) {}

    // ────────────────────────────────────────────────────────
    // GET /cart
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartResponse getCart() {
        User user = userService.getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        // Sync newest price from ProductSku for each item
        boolean priceChanged = false;
        for (CartItem item : cart.getItems()) {
            ProductSku sku = item.getProductSku();
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(sku.getPrice()) != 0) {
                item.setUnitPrice(sku.getPrice());
                priceChanged = true;
            }
        }

        if (priceChanged) {
            cartRepository.save(cart);
        }

        return cartMapper.toCartResponse(cart);
    }

    // ────────────────────────────────────────────────────────
    // DELETE /cart/items/:skuId
    // ────────────────────────────────────────────────────────
    @Transactional
    public void removeCartItem(Long skuId) {
        User user = userService.getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(cartItem);
    }

    // ────────────────────────────────────────────────────────
    // PATCH /cart/items/:skuId
    // ────────────────────────────────────────────────────────
    @Transactional
    public CartItemResponse updateCartItemQuantity(Long skuId, UpdateQuantityRequest request) {
        User user = userService.getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository
                .findByCartIdAndProductSkuId(cart.getId(), skuId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        int newQuantity = request.getQuantity();

        // Check stock
        int stock = inventoryService.getOnHandOrZero(cartItem.getProductSku().getId());
        if (newQuantity > stock) {
            throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
        }

        // Update quantity and sync newest price
        cartItem.setQuantity(newQuantity);
        cartItem.setUnitPrice(cartItem.getProductSku().getPrice());
        cartItem = cartItemRepository.save(cartItem);

        return cartItemMapper.toCartItemResponse(cartItem);
    }

    // ────────────────────────────────────────────────────────
    // DELETE /cart
    // ────────────────────────────────────────────────────────
    @Transactional
    public void clearCart() {
        User user = userService.getCurrentUser();

        Cart cart = cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
