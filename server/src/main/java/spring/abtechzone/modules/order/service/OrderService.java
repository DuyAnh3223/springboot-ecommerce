package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
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

    RedissonClient redissonClient;
    TransactionTemplate transactionTemplate;

    static BigDecimal FLAT_SHIPPING_FEE = BigDecimal.valueOf(30000);

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

        // Step 2: Get Actice Cart, existed and not null
        Cart cart = getActiveCart(user);
        validateCartNotEmpty(cart);

        // Step 3: Validate each CartItem
        List<CheckoutItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            ProductSku sku = cartItem.getProductSku();

            // Product existed & is selling?
            validateProductAvailable(sku);

            // stock?
            validateStock(sku, cartItem.getQuantity());

            items.add(orderMapper.toCheckoutItemResponse(cartItem));

            BigDecimal totalPrice = sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(totalPrice);
        }

        // Step 4: Validate & calc discount from voucher
        BigDecimal totalDiscount = BigDecimal.ZERO;
        String appliedVoucherCode = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            Voucher voucher = voucherRepository
                    .findByCode(request.getVoucherCode())
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            voucherValidator.validateVoucher(voucher, subtotal);
            totalDiscount = calculateDiscount(voucher, subtotal);
            appliedVoucherCode = voucher.getCode();
        }

        // Step 5: Calc shipping fee
        BigDecimal shippingFee = FLAT_SHIPPING_FEE;

        // Step 6: Calc total
        BigDecimal totalCheckout = subtotal.add(shippingFee).subtract(totalDiscount);
        if (totalCheckout.compareTo(BigDecimal.ZERO) < 0) {
            totalCheckout = BigDecimal.ZERO;
        }

        // Step 7: Return CheckoutResponse
        return CheckoutResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalDiscount(totalDiscount)
                .totalCheckout(totalCheckout)
                .voucherCode(appliedVoucherCode)
                .build();
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
        AppliedVoucherInfo voucherInfo = applyVoucher(request, user, processed.subtotal());

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

    private AppliedVoucherInfo applyVoucher(CreateOrderRequest request, User user, BigDecimal subtotal) {
        if (request.getVoucherCode() == null || request.getVoucherCode().isBlank()) {
            return new AppliedVoucherInfo(null, BigDecimal.ZERO);
        }

        Voucher appliedVoucher = voucherRepository
                .findByCode(request.getVoucherCode())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        voucherValidator.validateVoucher(appliedVoucher, subtotal);
        validateVoucherPerUser(appliedVoucher, user);
        BigDecimal discountAmount = calculateDiscount(appliedVoucher, subtotal);

        return new AppliedVoucherInfo(appliedVoucher, discountAmount);
    }

    private Order buildOrder(
            CreateOrderRequest request,
            User user,
            AddressInfo addressInfo,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            List<OrderItem> orderItems) {

        BigDecimal shippingFee = FLAT_SHIPPING_FEE;
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

    private void validateVoucherPerUser(Voucher voucher, User user) {
        if (voucher.getMaxPerUser() == null) return;

        long userUsageCount = voucher.getUserIds().stream()
                .filter(u -> u.getId().equals(user.getId()))
                .count();

        if (userUsageCount >= voucher.getMaxPerUser()) {
            throw new AppException(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED);
        }
    }

    /**
     * Tính discount dựa trên loại voucher.
     * - FIXED_AMOUNT: trả trực tiếp giá trị voucher
     * - PERCENTAGE: tính theo phần trăm, cap tại subtotal
     */
    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount = BigDecimal.ZERO;

        if (voucher.getType() == VoucherType.FIXED_AMOUNT) {
            discount = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
        } else if (voucher.getType() == VoucherType.PERCENTAGE) {
            BigDecimal percentage = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
            discount = subtotal.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Discount không được vượt quá subtotal
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount;
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
