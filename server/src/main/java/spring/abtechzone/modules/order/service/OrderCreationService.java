package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
import spring.abtechzone.modules.order.constant.PaymentStatus;
import spring.abtechzone.modules.order.dto.request.*;
import spring.abtechzone.modules.order.dto.response.*;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderItem;
import spring.abtechzone.modules.order.entity.OrderStatusHistory;
import spring.abtechzone.modules.order.mapper.OrderMapper;
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.Address;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.AddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.entity.VoucherRedemption;
import spring.abtechzone.modules.voucher.repository.VoucherRedemptionRepository;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderCreationService {

    private static final long LOCK_WAIT_SECONDS = 5;
    private static final int IDEMPOTENCY_RETRY_LIMIT = 3;

    UserRepository userRepository;
    CartRepository cartRepository;
    VoucherRepository voucherRepository;
    VoucherRedemptionRepository voucherRedemptionRepository;
    OrderRepository orderRepository;
    AddressRepository addressRepository;
    InventoryService inventoryService;
    OrderStatusHistoryRepository orderStatusHistoryRepository;
    ProductSkuRepository productSkuRepository;
    OrderMapper orderMapper;
    AuthService authService;
    CheckoutService checkoutService;

    RedissonClient redissonClient;
    TransactionTemplate transactionTemplate;

    /** Creates an order from a previously reviewed checkout snapshot. */
    public OrderResponse createOrder(CreateOrderRequest request, String rawIdempotencyKey) {
        // Step 1: Auth User (idempotency is scoped to the authenticated user)
        User user = getAuthenticatedUser();

        // Step 2: Canonical idempotency key and request hash
        String idempotencyKey = canonicalKey(rawIdempotencyKey);
        String requestHash = CreateOrderRequestHash.compute(request, user.getId());

        // Step 3: Replay lookup BEFORE reading the active cart (R-C04-01 / AC-C04-01)
        Order existing = orderRepository
                .findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return replayOrConflict(existing, requestHash);
        }

        // Step 4: Lock keys derive from the reviewed SKU IDs only (R-C04-02).
        // The cart is NOT read outside the transaction: with OSIV enabled the
        // pre-lock read would seed the persistence context, and a query inside
        // the transaction could then return the stale cart instead of the
        // current row. The cart is loaded and validated for the first time
        // inside the transaction, after locks are held (R-C04-03).
        List<Long> selectedSkuIds = deriveSelectedSkuIds(request);

        // Step 5: Lock keys from the selected SKUs only
        List<String> lockKeys = buildLockKeys(user, selectedSkuIds, request);
        List<RLock> locks = lockKeys.stream().map(redissonClient::getLock).toList();

        // Steps 6-8: Acquire every lock before the transaction and release it on
        // the same method path, including timeout, interruption and callback failure.
        return executeWithLocks(
                locks,
                0,
                () -> executeWithIdempotencyRetry(
                        () -> transactionTemplate.execute(
                                status -> doCreateOrder(request, user, idempotencyKey, requestHash)),
                        user,
                        idempotencyKey,
                        requestHash));
    }

    private OrderResponse executeWithIdempotencyRetry(
            TransactionCallback callback, User user, String idempotencyKey, String requestHash) {
        for (int attempt = 1; ; attempt++) {
            try {
                return callback.run();
            } catch (DataIntegrityViolationException e) {
                // Concurrent insert hit the unique (user_id, idempotency_key) constraint:
                // reload the winning order and replay-or-conflict; bounded retry, no infinite loop.
                Order concurrent = orderRepository
                        .findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                        .orElse(null);
                if (concurrent != null) {
                    return replayOrConflict(concurrent, requestHash);
                }
                if (attempt >= IDEMPOTENCY_RETRY_LIMIT) {
                    throw new AppException(ErrorCode.SYSTEM_ERROR);
                }
            }
        }
    }

    private OrderResponse doCreateOrder(
            CreateOrderRequest request, User user, String idempotencyKey, String requestHash) {
        // Step 1: Recheck idempotency inside the transaction, after locks
        Order replayed = orderRepository
                .findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey)
                .orElse(null);
        if (replayed != null) {
            return replayOrConflict(replayed, requestHash);
        }

        // Step 2: Reload active cart and selected items with authoritative state
        Cart freshCart = getActiveCart(user);
        validateCartNotEmpty(freshCart);
        List<Long> selectedSkuIds = deriveSelectedSkuIds(request);
        validateSelectionInCart(freshCart, selectedSkuIds);

        // Step 3: Recompute authoritative checkout review from server state
        CheckoutService.AuthoritativeCheckout authoritative =
                checkoutService.recomputeCheckout(user, selectedSkuIds, normalizeVoucherCode(request));

        // Step 4: Semantic-compare reviewed snapshot against the authoritative review (R-C04-07)
        CheckoutChangedException mismatch =
                checkoutService.findMismatch(request, selectedSkuIds, freshCart, authoritative);
        if (mismatch != null) {
            throw mismatch;
        }

        // Step 5: Resolve address (existing must belong to user; new address must pass validation)
        AddressInfo addressInfo = resolveAddress(request, user);

        // Step 6: Build order with authoritative amounts only (never client money)
        Order order = buildOrder(request, user, addressInfo, idempotencyKey, requestHash, authoritative);

        // Step 7: Save order (cascade saves items) + initial history fromStatus=null -> toStatus=PENDING
        Order savedOrder = orderRepository.save(order);
        createOrderStatusHistory(savedOrder, user);

        // Step 8: Atomic stock decrement + SALE_OUT movement, per sorted SKU.
        // Persisted OrderItems are the committed quantity source (ADR-003).
        allocateInventory(authoritative, savedOrder);

        // Step 9: Voucher atomic redemption (guarded increment + per-user recheck + REDEEMED ledger)
        if (authoritative.voucher() != null) {
            redeemVoucher(authoritative.voucher(), user, savedOrder);
        }

        // Step 10: Remove exactly the selected cart items; keep ACTIVE if any remain, else COMPLETED
        removeSelectedCartItems(freshCart, selectedSkuIds);

        // Step 11: Flush at a point that surfaces constraint violations inside the transaction
        orderRepository.flush();

        return orderMapper.toOrderResponse(savedOrder);
    }

    private OrderResponse executeWithLocks(List<RLock> locks, int index, TransactionCallback callback) {
        if (index >= locks.size()) {
            return callback.run();
        }

        RLock lock = locks.get(index);
        return executeWithLock(lock, () -> executeWithLocks(locks, index + 1, callback));
    }

    private OrderResponse executeWithLock(RLock lock, TransactionCallback callback) {
        try {
            if (!lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AppException(ErrorCode.SYSTEM_BUSY);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.SYSTEM_BUSY);
        }

        try {
            return callback.run();
        } finally {
            lock.unlock();
        }
    }

    private OrderResponse replayOrConflict(Order existing, String requestHash) {
        if (existing.getRequestHash().equals(requestHash)) {
            return orderMapper.toOrderResponse(existing);
        }
        throw new AppException(ErrorCode.IDEMPOTENCY_KEY_REUSED);
    }

    /**
     * Returns a 409 CHECKOUT_CHANGED exception when any order-affecting value in the
     * reviewed snapshot differs from the authoritative review, else null.
     */
    private void allocateInventory(CheckoutService.AuthoritativeCheckout authoritative, Order order) {
        for (CheckoutService.AuthoritativeLine line : authoritative.lines()) {
            ProductSku sku = productSkuRepository
                    .findById(line.skuId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            inventoryService.decreaseStock(sku, line.quantity(), order);
        }
    }

    private void redeemVoucher(Voucher voucher, User user, Order order) {
        // Atomic guarded increment (max uses + per-user count) inside the voucher lock
        int updated = voucherRepository.increaseUsedCount(voucher.getId(), user.getId());
        if (updated == 0) {
            throw new AppException(ErrorCode.VOUCHER_ARE_OUT);
        }
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(voucher);
        redemption.setUser(user);
        redemption.setOrder(order);
        redemption.setStatus(VoucherRedemptionStatus.REDEEMED);
        redemption.setCreatedAt(OffsetDateTime.now());
        voucherRedemptionRepository.save(redemption);
    }

    private void removeSelectedCartItems(Cart cart, List<Long> selectedSkuIds) {
        cart.getItems()
                .removeIf(item -> selectedSkuIds.contains(item.getProductSku().getId()));
        if (cart.getItems().isEmpty()) {
            cart.setStatus(CartStatus.COMPLETED);
        }
        cartRepository.save(cart);
    }

    private Order buildOrder(
            CreateOrderRequest request,
            User user,
            AddressInfo addressInfo,
            String idempotencyKey,
            String requestHash,
            CheckoutService.AuthoritativeCheckout authoritative) {

        BigDecimal subtotal = authoritative.subtotal();
        BigDecimal discountAmount = authoritative.response().getDiscountAmount();
        BigDecimal shippingFee = authoritative.response().getShippingFee();
        BigDecimal totalCheckout = authoritative.response().getTotalAmount();

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .subtotalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalCheckout)
                .recipientName(addressInfo.recipientName)
                .phone(addressInfo.phone)
                .fullAddress(addressInfo.fullAddress)
                .userId(user.getId())
                .shippingAddressId(addressInfo.addressId)
                .voucherCode(authoritative.normalizedVoucherCode())
                .voucher(authoritative.voucher())
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .items(new ArrayList<>())
                .build();

        for (CheckoutService.AuthoritativeLine line : authoritative.lines()) {
            ProductSku sku = productSkuRepository
                    .findById(line.skuId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .sku(sku)
                    .skuId(sku.getId())
                    .productNameSnapshot(
                            sku.getProduct() != null ? sku.getProduct().getName() : null)
                    .skuSnapshot(sku.getSku())
                    .unitPrice(line.unitPrice())
                    .quantity(line.quantity())
                    .imageUrl(sku.getImageUrl())
                    .build();
            order.getItems().add(orderItem);
        }

        return order;
    }

    private void createOrderStatusHistory(Order order, User user) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PENDING.name());
        history.setFromStatus(null);
        history.setToStatus(OrderStatus.PENDING.name());
        history.setActorType(OrderStatusHistoryActor.CUSTOMER.name());
        history.setActorId(user.getId().toString());
        history.setNote("Order created");
        history.setCreatedBy(user);
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private User getAuthenticatedUser() {
        String username = authService.getCurrentUsername();
        return userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Cart getActiveCart(User user) {
        return cartRepository
                .findByUserIdAndStatus(user.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));
    }

    private void validateCartNotEmpty(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new AppException(ErrorCode.CART_IS_EMPTY);
        }
    }

    private List<Long> deriveSelectedSkuIds(CreateOrderRequest request) {
        return request.getReviewedCheckout().getItems().stream()
                .map(ReviewedCheckoutItemRequest::getSkuId)
                .distinct()
                .sorted()
                .toList();
    }

    private void validateSelectionInCart(Cart cart, List<Long> selectedSkuIds) {
        Map<Long, CartItem> cartItemBySkuId = cart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductSku().getId(), item -> item, (a, b) -> a));
        for (Long skuId : selectedSkuIds) {
            if (!cartItemBySkuId.containsKey(skuId)) {
                throw new AppException(ErrorCode.CART_ITEM_NOT_IN_CART);
            }
        }
    }

    private List<String> buildLockKeys(User user, List<Long> selectedSkuIds, CreateOrderRequest request) {
        List<String> keys = new ArrayList<>();
        keys.add("lock:user-order:" + user.getId());
        for (Long skuId : selectedSkuIds) {
            keys.add("lock:product-sku:" + skuId);
        }
        String voucherCode = normalizeVoucherCode(request);
        if (voucherCode != null) {
            keys.add("lock:voucher:" + voucherCode);
        }
        return keys.stream().distinct().sorted().toList();
    }

    private String normalizeVoucherCode(CreateOrderRequest request) {
        if (request.getReviewedCheckout() == null
                || request.getReviewedCheckout().getVoucher() == null) {
            return null;
        }
        String code = request.getReviewedCheckout().getVoucher().getCode();
        if (code == null || code.isBlank()) {
            return null;
        }
        return normalizeCode(code);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String canonicalKey(String rawKey) {
        try {
            return UUID.fromString(rawKey).toString();
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    /** Resolves exactly one saved or inline shipping address. */
    private AddressInfo resolveAddress(CreateOrderRequest request, User user) {
        boolean hasAddressId = request.getAddressId() != null;
        boolean hasNewAddress = request.getNewUserAddress() != null;
        if (hasAddressId == hasNewAddress) {
            throw new AppException(ErrorCode.ADDRESS_REQUIRED);
        }

        if (hasAddressId) {
            Address address = addressRepository
                    .findById(request.getAddressId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

            if (!address.getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.ADDRESS_NOT_BELONG_TO_USER);
            }

            String fullAddress = String.join(", ", address.getStreet(), address.getWard(), address.getProvince());

            return new AddressInfo(address.getId(), address.getRecipientName(), address.getPhone(), fullAddress);

        } else {
            AddressRequest addr = request.getNewUserAddress();

            UUID savedAddressId = null;
            if (addr.isSaveAddress()) {
                Address newAddress = Address.builder()
                        .recipientName(addr.getRecipientName())
                        .phone(addr.getPhone())
                        .province(addr.getProvince())
                        .ward(addr.getWard())
                        .street(addr.getStreet())
                        .isDefault(false)
                        .user(user)
                        .build();
                Address saved = addressRepository.save(newAddress);
                savedAddressId = saved.getId();
            }

            String fullAddress = String.join(", ", addr.getStreet(), addr.getWard(), addr.getProvince());

            return new AddressInfo(savedAddressId, addr.getRecipientName(), addr.getPhone(), fullAddress);
        }
    }

    /** Generates an order code in the form ORD-yyyyMMdd-XXXXXXXX. */
    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    private enum OrderStatusHistoryActor {
        CUSTOMER
    }

    @FunctionalInterface
    private interface TransactionCallback {
        OrderResponse run();
    }

    private record AddressInfo(UUID addressId, String recipientName, String phone, String fullAddress) {}
}
