package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.catalog.entity.ProductSku;
import spring.abtechzone.modules.catalog.repository.ProductSkuRepository;
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
import spring.abtechzone.modules.order.repository.OrderRepository;
import spring.abtechzone.modules.order.repository.OrderStatusHistoryRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.entity.UserAddress;
import spring.abtechzone.modules.user.repository.UserAddressRepository;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    UserRepository userRepository;
    CartRepository cartRepository;
    VoucherRepository voucherRepository;
    ProductSkuRepository productSkuRepository;
    OrderRepository orderRepository;
    UserAddressRepository userAddressRepository;
    VoucherValidator voucherValidator;
    spring.abtechzone.modules.inventory.service.InventoryService inventoryService;
    OrderStatusHistoryRepository orderStatusHistoryRepository;

    static BigDecimal FLAT_SHIPPING_FEE = BigDecimal.valueOf(30000);

    // ────────────────────────────────────────────────────────
    // 0. Get Orders by User ID
    // ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(order -> OrderResponse.builder()
                        .orderId(order.getId())
                        .orderCode(order.getOrderCode())
                        .orderStatus(order.getStatus().name())
                        .subtotal(order.getSubtotalAmount())
                        .shippingFee(order.getShippingFee())
                        .totalDiscount(order.getDiscountAmount())
                        .totalCheckout(order.getTotalAmount())
                        .build())
                .toList();
    }

    // ────────────────────────────────────────────────────────
    // 1. Checkout Review — READ-ONLY, không tạo order
    // ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CheckoutResponse checkoutReview(CheckoutRequest request) {
        // Step 1-2: Xác định User đang đăng nhập
        User user = getAuthenticatedUser();

        // Step 3-5: Lấy Cart ACTIVE, validate tồn tại & không rỗng
        Cart cart = getActiveCart(user);
        validateCartNotEmpty(cart);

        // Step 9: Duyệt từng CartItem — validate & build response items
        List<CheckoutItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            ProductSku sku = cartItem.getProductSku();

            // Product còn tồn tại & còn bán?
            validateProductAvailable(sku);

            // Đủ tồn kho?
            validateStock(sku, cartItem.getQuantity());

            // Sync lại giá mới nhất từ ProductSku
            BigDecimal unitPrice = sku.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            items.add(CheckoutItemResponse.builder()
                    .productSkuId(sku.getId())
                    .productName(sku.getProduct().getName())
                    .skuCode(sku.getSku())
                    .imageUrl(sku.getImageUrl())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .build());

            subtotal = subtotal.add(totalPrice);
        }

        // Step 8: Validate & tính discount từ voucher (nếu có)
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

        // Step 12: Tính shipping fee
        BigDecimal shippingFee = FLAT_SHIPPING_FEE;

        // Step 13: Tính total
        BigDecimal totalCheckout = subtotal.add(shippingFee).subtract(totalDiscount);
        if (totalCheckout.compareTo(BigDecimal.ZERO) < 0) {
            totalCheckout = BigDecimal.ZERO;
        }

        // Step 20: Return CheckoutResponse
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
    // 2. Create Order — TẠO ĐƠN HÀNG
    // ────────────────────────────────────────────────────────
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Step 1-2: Xác định User
        User user = getAuthenticatedUser();

        // Step 3-5: Lấy Cart ACTIVE, validate
        Cart cart = getActiveCart(user);
        validateCartNotEmpty(cart);

        // Step 6-7: Resolve địa chỉ giao hàng
        AddressInfo addressInfo = resolveAddress(request, user);

        // Step 9: Validate lại từng CartItem + tính subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            ProductSku sku = cartItem.getProductSku();

            validateProductAvailable(sku);
            validateStock(sku, cartItem.getQuantity());

            BigDecimal unitPrice = sku.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            orderItems.add(OrderItem.builder()
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .productNameSnapshot(sku.getProduct().getName())
                    .skuSnapshot(sku.getSku())
                    .imageUrl(sku.getImageUrl())
                    .sku(sku)
                    .build());

            subtotal = subtotal.add(totalPrice);
        }

        // Step 8: Validate & áp dụng Voucher
        Voucher appliedVoucher = null;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            appliedVoucher = voucherRepository
                    .findByCode(request.getVoucherCode())
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            voucherValidator.validateVoucher(appliedVoucher, subtotal);
            validateVoucherPerUser(appliedVoucher, user);
            totalDiscount = calculateDiscount(appliedVoucher, subtotal);
        }

        // Step 12-13: Tính shipping & total
        BigDecimal shippingFee = FLAT_SHIPPING_FEE;
        BigDecimal totalCheckout = subtotal.add(shippingFee).subtract(totalDiscount);
        if (totalCheckout.compareTo(BigDecimal.ZERO) < 0) {
            totalCheckout = BigDecimal.ZERO;
        }

        // Step 14: Tạo Order
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .status(OrderStatus.PENDING)
                .paymentReference(request.getPaymentMethod())
                .subtotalAmount(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(totalDiscount)
                .totalAmount(totalCheckout)
                .recipientName(addressInfo.recipientName)
                .phone(addressInfo.phone)
                .fullAddress(addressInfo.fullAddress)
                .userId(user.getId())
                .shippingAddressId(addressInfo.addressId)
                .voucherCode(request.getVoucherCode())
                .items(new ArrayList<>())
                .build();

        // Step 15: Tạo OrderItem + set quan hệ 2 chiều
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(order);
            order.getItems().add(orderItem);
        }

        // Step 18: Đánh dấu Cart COMPLETED
        cart.setStatus(CartStatus.COMPLETED);
        cartRepository.save(cart);

        // Step 17: Cập nhật Voucher đã sử dụng
        if (appliedVoucher != null) {
            int usedCount = appliedVoucher.getUsedCount() != null ? appliedVoucher.getUsedCount() : 0;
            appliedVoucher.setUsedCount(usedCount + 1);
            appliedVoucher.getUserIds().add(user);
            voucherRepository.save(appliedVoucher);
        }

        // Step 19: Lưu Order (cascade saves OrderItems)
        Order savedOrder = orderRepository.save(order);

        // Save status history audit
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setStatus(OrderStatus.PENDING.name());
        history.setNote("Order created");
        history.setCreatedBy(user);
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);

        // Step 16: Trừ tồn kho & Tạo reservation
        for (CartItem cartItem : cart.getItems()) {
            inventoryService.reserveStock(cartItem.getProductSku(), cartItem.getQuantity(), savedOrder);
        }

        // Step 20: Return OrderResponse
        return OrderResponse.builder()
                .orderId(savedOrder.getId())
                .orderCode(savedOrder.getOrderCode())
                .orderStatus(savedOrder.getStatus().name())
                .subtotal(savedOrder.getSubtotalAmount())
                .shippingFee(savedOrder.getShippingFee())
                .totalDiscount(savedOrder.getDiscountAmount())
                .totalCheckout(savedOrder.getTotalAmount())
                .build();
    }

    // ════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

    private User getAuthenticatedUser() {
        var context = SecurityContextHolder.getContext();
        String username = context.getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Cart getActiveCart(User user) {
        return cartRepository
                .findByUserId(user.getId())
                .filter(cart -> cart.getStatus() == CartStatus.ACTIVE)
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
            UserAddress userAddress = userAddressRepository
                    .findById(request.getAddressId())
                    .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

            if (!userAddress.getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.ADDRESS_NOT_BELONG_TO_USER);
            }

            String fullAddress = String.join(
                    ", ",
                    userAddress.getStreetAddress(),
                    userAddress.getWard(),
                    userAddress.getDistrict(),
                    userAddress.getProvince());

            return new AddressInfo(
                    userAddress.getId(), userAddress.getRecipientName(), userAddress.getPhone(), fullAddress);

        } else if (request.getNewUserAddress() != null) {
            // User mới: nhận địa chỉ từ request
            AddressRequest addr = request.getNewUserAddress();

            UUID savedAddressId = null;
            // Tùy chọn lưu địa chỉ mới
            if (addr.isSaveAddress()) {
                UserAddress newUserAddress = UserAddress.builder()
                        .recipientName(addr.getRecipientName())
                        .phone(addr.getPhone())
                        .province(addr.getProvince())
                        .district(addr.getDistrict())
                        .ward(addr.getWard())
                        .streetAddress(addr.getStreetAddress())
                        .isDefault(false)
                        .user(user)
                        .build();
                UserAddress saved = userAddressRepository.save(newUserAddress);
                savedAddressId = saved.getId();
            }

            String fullAddress =
                    String.join(", ", addr.getStreetAddress(), addr.getWard(), addr.getDistrict(), addr.getProvince());

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
