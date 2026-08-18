package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.dto.request.AddressRequest;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutItemResponse;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.dto.response.VoucherReviewResponse;
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
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.service.VoucherService;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    UserRepository userRepository;
    CartRepository cartRepository;
    VoucherRepository voucherRepository;
    OrderRepository orderRepository;
    AddressRepository addressRepository;
    VoucherValidator voucherValidator;
    InventoryService inventoryService;
    OrderStatusHistoryRepository orderStatusHistoryRepository;
    ProductSkuRepository productSkuRepository;
    OrderMapper orderMapper;
    AuthService authService;
    VoucherService voucherService;

    RedissonClient redissonClient;
    TransactionTemplate transactionTemplate;

    BigDecimal checkoutShippingFee = BigDecimal.valueOf(30000);

    // ────────────────────────────────────────────────────────
    // 0. Get Orders by User ID
    // ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────
    // 1. Checkout Review — READ-ONLY
    // ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CheckoutResponse checkoutReview(CheckoutRequest request) {
        // Step 1: Auth User
        User user = getAuthenticatedUser();

        // Step 1b: Selection must be present, non-empty and positive (service-level guard for non-controller callers)
        if (request.getSelectedSkuIds() == null
                || request.getSelectedSkuIds().isEmpty()
                || request.getSelectedSkuIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        // Step 2: Get active cart
        Cart cart = getActiveCart(user);

        // Step 3: Normalize selection: deduplicate + sort ascending
        List<Long> selectedSkuIds =
                request.getSelectedSkuIds().stream().distinct().sorted().toList();

        // Step 4: Map cart items by SKU ID; every selected ID must belong to the active cart
        Map<Long, CartItem> cartItemBySkuId = cart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductSku().getId(), item -> item, (a, b) -> a));

        List<CartItem> selectedItems = new ArrayList<>();
        for (Long skuId : selectedSkuIds) {
            CartItem cartItem = cartItemBySkuId.get(skuId);
            if (cartItem == null) {
                // Ownership-safe: selected SKU not in this user's active cart (400, never leaks other carts)
                throw new AppException(ErrorCode.CART_ITEM_NOT_IN_CART);
            }
            selectedItems.add(cartItem);
        }

        // Step 5: Evaluate each selected line: sellability issues are typed review results, not exceptions
        List<CheckoutItemResponse> items = new ArrayList<>();
        Map<Long, BigDecimal> skuSubtotals = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allLinesSellable = true;

        for (CartItem cartItem : selectedItems) {
            Long skuId = cartItem.getProductSku().getId();
            String issueCode = null;
            ProductSku freshSku = null;
            BigDecimal lineTotal = null;

            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                issueCode = ErrorCode.CART_ITEM_QUANTITY_INVALID.name();
            } else {
                // Re-fetch SKU from DB for authoritative price/stock/sellability
                freshSku = productSkuRepository.findById(skuId).orElse(null);

                if (freshSku == null) {
                    issueCode = ErrorCode.SKU_NOT_FOUND.name();
                } else if (!freshSku.isActive()) {
                    issueCode = ErrorCode.PRODUCT_NOT_AVAILABLE.name();
                } else if (freshSku.getProduct() == null
                        || !freshSku.getProduct().isPublished()
                        || freshSku.getProduct().isDraft()) {
                    issueCode = ErrorCode.PRODUCT_NOT_AVAILABLE.name();
                } else if (freshSku.getStock() == null || freshSku.getStock() < cartItem.getQuantity()) {
                    issueCode = ErrorCode.INSUFFICIENT_STOCK.name();
                }

                if (freshSku != null && freshSku.getPrice() != null) {
                    // lineTotal = database unit price × cart quantity, sellable or not
                    lineTotal = freshSku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                    subtotal = subtotal.add(lineTotal);
                    skuSubtotals.merge(skuId, lineTotal, BigDecimal::add);
                }
            }

            if (issueCode != null) {
                allLinesSellable = false;
            }

            CheckoutItemResponse.CheckoutItemResponseBuilder itemBuilder =
                    CheckoutItemResponse.builder().skuId(skuId).quantity(cartItem.getQuantity());
            if (freshSku != null) {
                itemBuilder
                        .skuCode(freshSku.getSku())
                        .productName(
                                freshSku.getProduct() != null
                                        ? freshSku.getProduct().getName()
                                        : null)
                        .imageUrl(freshSku.getImageUrl())
                        .unitPrice(freshSku.getPrice())
                        .availableStock(freshSku.getStock());
            }
            if (lineTotal != null) {
                itemBuilder.lineTotal(lineTotal);
            }
            items.add(itemBuilder.issueCode(issueCode).build());
        }

        // Step 6: Evaluate voucher (typed review result; invalid voucher is expected, not an exception)
        VoucherReview voucherReview = evaluateVoucherReview(request.getVoucherCode(), user, skuSubtotals, subtotal);

        // Step 7: Shipping fee — property-backed, always part of the breakdown
        BigDecimal shippingFee = checkoutShippingFee;
        boolean canPlaceOrder = allLinesSellable && voucherReview.applicable();

        // Step 8: Total — server-authoritative, consistent with the displayed shipping fee, never negative
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(voucherReview.discountAmount());
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // Step 9: Reviewed snapshot (no fingerprint/token/expiry)
        return CheckoutResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .eligibleSubtotal(voucherReview.eligibleSubtotal())
                .shippingFee(shippingFee)
                .discountAmount(voucherReview.discountAmount())
                .totalAmount(totalAmount)
                .voucher(
                        voucherReview.normalizedCode() == null
                                ? null
                                : VoucherReviewResponse.builder()
                                        .code(voucherReview.normalizedCode())
                                        .applicable(voucherReview.applicable())
                                        .issueCode(voucherReview.issueCode())
                                        .build())
                .canPlaceOrder(canPlaceOrder)
                .build();
    }

    private record VoucherReview(
            String normalizedCode,
            BigDecimal eligibleSubtotal,
            BigDecimal discountAmount,
            boolean applicable,
            String issueCode) {}

    private VoucherReview evaluateVoucherReview(
            String rawVoucherCode, User user, Map<Long, BigDecimal> skuSubtotals, BigDecimal subtotal) {
        if (rawVoucherCode == null || rawVoucherCode.isBlank()) {
            return new VoucherReview(null, subtotal, BigDecimal.ZERO, true, null);
        }

        String normalizedCode = rawVoucherCode.trim().toUpperCase(java.util.Locale.ROOT);

        try {
            Voucher voucher = voucherRepository
                    .findByCode(normalizedCode)
                    .or(() -> voucherRepository.findByCode(rawVoucherCode))
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            BigDecimal eligibleSubtotal = voucherService.calculateEligibleSubtotal(voucher, skuSubtotals, subtotal);
            voucherValidator.validateForCheckout(voucher, user, subtotal, eligibleSubtotal);

            BigDecimal discountAmount = voucherService.getDiscount(voucher, eligibleSubtotal);
            return new VoucherReview(normalizedCode, eligibleSubtotal, discountAmount, true, null);
        } catch (AppException e) {
            return new VoucherReview(
                    normalizedCode,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    e.getErrorCode().name());
        }
    }

    // ────────────────────────────────────────────────────────
    // 2. Create Order
    // ────────────────────────────────────────────────────────
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Step 1: Auth User
        User user = getAuthenticatedUser();

        // Step 2: Get Cart ACTIVE and skus for locking
        Cart initialCart = getActiveCart(user);
        validateCartNotEmpty(initialCart);

        // Collect all lock keys reliable to this transaction
        List<String> lockKeys = new ArrayList<>();

        // Lock 1: User Order Lock (Prohibit double-click / double-submit from same user)
        lockKeys.add("lock:user-order:" + user.getId());

        // Lock 2: SKU Locks (Race condition stock)
        for (CartItem item : initialCart.getItems()) {
            lockKeys.add("lock:product-sku:" + item.getProductSku().getId());
        }

        // Lock 3: Voucher Lock (Race condition oversell voucher / over max uses)
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            lockKeys.add("lock:voucher:" + request.getVoucherCode());
        }

        // Deduplicate and sort to alphabet for no deadlock
        List<String> sortedLockKeys = lockKeys.stream().distinct().sorted().toList();

        List<RLock> locks = sortedLockKeys.stream().map(redissonClient::getLock).toList();

        // Save SKU và default stock for comparetion after locking
        Map<Long, Integer> initialSkuQtyMap = initialCart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductSku().getId(), CartItem::getQuantity, Integer::sum));

        try {
            // Step 4: try to acquire Locks
            for (RLock lock : locks) {
                // Wait max 5s to get lock, free after 10s if crash
                boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new AppException(ErrorCode.SYSTEM_BUSY);
                }
            }

            // Step 5: Call createOrder in Transaction through TransactionTemplate

            return transactionTemplate.execute(status -> doCreateOrder(request, user, initialSkuQtyMap));

        } catch (Exception e) {
            if (e instanceof AppException appException) {
                throw appException;
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        } finally {
            // Step 6: Free all lock
            for (RLock lock : locks) {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private OrderResponse doCreateOrder(CreateOrderRequest request, User user, Map<Long, Integer> initialSkuQtyMap) {
        // Step 1: Reload Cart & Validate Cart State
        Cart freshCart = getActiveCart(user);
        validateCartState(freshCart, initialSkuQtyMap);

        // Step 2: Resolve Shipping Address
        AddressInfo addressInfo = resolveAddress(request, user);

        // Step 3: Process Cart Items (validate stock & price, calculate subtotal)
        ProcessedItems processed = processCartItems(freshCart);

        // Step 4: Validate & Apply Voucher
        Map<Long, BigDecimal> skuSubtotals = new HashMap<>();
        for (OrderItem item : processed.orderItems()) {
            BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            skuSubtotals.merge(item.getSku().getId(), itemTotal, BigDecimal::add);
        }
        AppliedVoucherInfo voucherInfo =
                applyVoucher(request.getVoucherCode(), user, skuSubtotals, processed.subtotal());

        // Step 5: Build Order & Link Order Items
        Order order = buildOrder(
                request, user, addressInfo, processed.subtotal(), voucherInfo.discountAmount(), processed.orderItems());

        // Step 6: Clear Cart items and mark as COMPLETED
        freshCart.getItems().clear();
        freshCart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(freshCart);

        // Step 7: Update Voucher usage
        updateVoucherUsage(voucherInfo.voucher(), user);

        // Step 8: Save Order (cascade saves OrderItems)
        Order savedOrder = orderRepository.save(order);

        // Step 9: Save Order Status History
        createOrderStatusHistory(savedOrder, user);

        // Step 10: Reserve Inventory (uses processed.orderItems instead of empty cart)
        reserveInventory(processed.orderItems(), processed.skuMap(), savedOrder);

        // Step 11: Return Response
        return orderMapper.toOrderResponse(savedOrder);
    }

    private void validateCartState(Cart freshCart, Map<Long, Integer> initialSkuQtyMap) {
        validateCartNotEmpty(freshCart);

        Map<Long, Integer> freshSkuQtyMap = freshCart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductSku().getId(), CartItem::getQuantity, Integer::sum));

        if (!initialSkuQtyMap.equals(freshSkuQtyMap)) {
            throw new AppException(ErrorCode.SYSTEM_BUSY);
        }
    }

    private ProcessedItems processCartItems(Cart freshCart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, ProductSku> skuMap = new HashMap<>();

        for (CartItem cartItem : freshCart.getItems()) {
            // Re-fetch ProductSku from DB for newest  Price & Stock
            ProductSku sku = productSkuRepository
                    .findById(cartItem.getProductSku().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            skuMap.put(sku.getId(), sku);

            validateProductAvailable(sku);
            validateStock(sku, cartItem.getQuantity());

            orderItems.add(orderMapper.toOrderItem(cartItem, sku));

            BigDecimal totalPrice = sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(totalPrice);
        }

        return new ProcessedItems(orderItems, subtotal, skuMap);
    }

    private AppliedVoucherInfo applyVoucher(
            String voucherCode, User user, Map<Long, BigDecimal> skuSubtotals, BigDecimal subtotal) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return new AppliedVoucherInfo(null, BigDecimal.ZERO);
        }

        Voucher voucher = loadAndValidateCheckoutVoucher(voucherCode, user, skuSubtotals, subtotal);
        BigDecimal eligibleSubtotal = voucherService.calculateEligibleSubtotal(voucher, skuSubtotals, subtotal);
        BigDecimal discountAmount = voucherService.getDiscount(voucher, eligibleSubtotal);

        return new AppliedVoucherInfo(voucher, discountAmount);
    }

    private Voucher loadAndValidateCheckoutVoucher(
            String voucherCode, User user, Map<Long, BigDecimal> skuSubtotals, BigDecimal fullSubtotal) {
        String normalizedCode = voucherCode != null ? voucherCode.trim().toUpperCase(java.util.Locale.ROOT) : null;
        Voucher voucher = voucherRepository
                .findByCode(normalizedCode)
                .or(() -> voucherRepository.findByCode(voucherCode))
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        BigDecimal eligibleSubtotal = voucherService.calculateEligibleSubtotal(voucher, skuSubtotals, fullSubtotal);
        voucherValidator.validateForCheckout(voucher, user, fullSubtotal, eligibleSubtotal);
        return voucher;
    }

    private Order buildOrder(
            CreateOrderRequest request,
            User user,
            AddressInfo addressInfo,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            List<OrderItem> orderItems) {

        BigDecimal shippingFee = checkoutShippingFee;
        BigDecimal totalCheckout = subtotal.add(shippingFee).subtract(discountAmount);
        if (totalCheckout.compareTo(BigDecimal.ZERO) < 0) {
            totalCheckout = BigDecimal.ZERO;
        }

        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .status(OrderStatus.PENDING)
                .paymentReference(request.getPaymentMethod())
                .subtotalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .totalAmount(totalCheckout)
                .recipientName(addressInfo.recipientName)
                .phone(addressInfo.phone)
                .fullAddress(addressInfo.fullAddress)
                .userId(user.getId())
                .shippingAddressId(addressInfo.addressId)
                .voucherCode(request.getVoucherCode())
                .items(new ArrayList<>())
                .build();

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        return order;
    }

    private void updateVoucherUsage(Voucher appliedVoucher, User user) {
        if (appliedVoucher == null) return;

        int updated = voucherRepository.increaseUsedCount(appliedVoucher.getId(), user.getId());
        if (updated == 0) {
            throw new AppException(ErrorCode.VOUCHER_ARE_OUT);
        }
        voucherRepository.insertVoucherUser(appliedVoucher.getId(), user.getId());
    }

    private void createOrderStatusHistory(Order order, User user) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.PENDING.name());
        history.setNote("Order created");
        history.setCreatedBy(user);
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private void reserveInventory(List<OrderItem> orderItems, Map<Long, ProductSku> skuMap, Order order) {
        for (OrderItem orderItem : orderItems) {
            ProductSku sku = skuMap.get(orderItem.getSku().getId());
            inventoryService.reserveStock(sku, orderItem.getQuantity(), order);
        }
    }

    private record ProcessedItems(List<OrderItem> orderItems, BigDecimal subtotal, Map<Long, ProductSku> skuMap) {}

    private record AppliedVoucherInfo(Voucher voucher, BigDecimal discountAmount) {}

    // ────────────────────────────────────────────────────────
    // 3. Cancel Order by User
    // ────────────────────────────────────────────────────────

    // ────────────────────────────────────────────────────────
    // 4. Update Order Status by Admin
    // ────────────────────────────────────────────────────────

    // ════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

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

    private void validateProductAvailable(ProductSku sku) {
        if (sku.getProduct() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!sku.getProduct().isPublished()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
    }

    private void validateStock(ProductSku sku, int requestedQuantity) {
        if (sku.getStock() == null || sku.getStock() < requestedQuantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    /**
     * Resolve địa chỉ giao hàng:
     * - Nếu có addressId → dùng địa chỉ đã lưu
     * - Nếu có newAddress → dùng địa chỉ mới (tùy chọn lưu lại)
     */
    private AddressInfo resolveAddress(CreateOrderRequest request, User user) {
        if (request.getAddressId() != null) {
            // User cũ: chọn địa chỉ đã lưu
            Address address = addressRepository
                    .findById(request.getAddressId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

            if (!address.getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.ADDRESS_NOT_BELONG_TO_USER);
            }

            String fullAddress = String.join(", ", address.getStreet(), address.getWard(), address.getProvince());

            return new AddressInfo(address.getId(), address.getRecipientName(), address.getPhone(), fullAddress);

        } else if (request.getNewUserAddress() != null) {
            // User mới: nhận địa chỉ từ request
            AddressRequest addr = request.getNewUserAddress();

            UUID savedAddressId = null;
            // Tùy chọn lưu địa chỉ mới
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

        } else {
            throw new AppException(ErrorCode.ADDRESS_REQUIRED);
        }
    }

    /**
     * Generate mã đơn hàng unique: ORD-yyyyMMdd-XXXX
     */
    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    /**
     * Record nội bộ chứa thông tin địa chỉ đã resolve
     */
    private record AddressInfo(UUID addressId, String recipientName, String phone, String fullAddress) {}
}
