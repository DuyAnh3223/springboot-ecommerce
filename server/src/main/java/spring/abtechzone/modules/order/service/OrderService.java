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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.CheckoutChangedException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.Cart;
import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.cart.repository.CartRepository;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
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
import spring.abtechzone.modules.order.service.OrderTransitionPolicy.Actor;
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
import spring.abtechzone.modules.voucher.service.VoucherService;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderService {

    private static final long LOCK_WAIT_SECONDS = 5;
    private static final int IDEMPOTENCY_RETRY_LIMIT = 3;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String CREATE_AT = "createdAt";

    UserRepository userRepository;
    CartRepository cartRepository;
    VoucherRepository voucherRepository;
    VoucherRedemptionRepository voucherRedemptionRepository;
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

    StockMovementRepository stockMovementRepository;

    @Value("${app.checkout.shipping-fee:30000}")
    BigDecimal checkoutShippingFee = BigDecimal.valueOf(30000);

    // ────────────────────────────────────────────────────────
    // 0b. Order Lifecycle: Customer reads, Cancel, Admin Transitions
    // ────────────────────────────────────────────────────────

    /**
     * Customer order list: current user, optional status filter, newest first.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size, User user) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Pageable pageable =
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(CREATE_AT).descending());
        Page<Order> orders = status == null
                ? orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                : orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), status, pageable);
        return orders.map(order -> toSummary(order, Actor.CUSTOMER));
    }

    /**
     * Customer order detail: owner-safe, non-owned code returns 404 (R-C05-02).
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getMyOrderDetail(String orderCode, User user) {
        Order order = orderRepository
                .findWithItemsByOrderCodeAndUserId(orderCode, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        OrderDetailResponse detail = toDetail(order, Actor.CUSTOMER);
        detail.setHistory(orderStatusHistoryRepository.findByOrderIdOrdered(order.getId()).stream()
                .map(orderMapper::toOrderHistoryResponse)
                .toList());
        return detail;
    }

    /**
     * Admin order detail: full snapshot with history.
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getAdminOrderDetail(String orderCode) {
        Order order = orderRepository
                .findWithItemsByOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        OrderDetailResponse detail = toDetail(order, Actor.ADMIN);
        detail.setHistory(orderStatusHistoryRepository.findByOrderIdOrdered(order.getId()).stream()
                .map(orderMapper::toOrderHistoryResponse)
                .toList());
        return detail;
    }

    private OrderSummaryResponse toSummary(Order order, Actor actor) {
        OrderSummaryResponse summary = orderMapper.toOrderSummaryResponse(order);
        summary.setAllowedTransitions(OrderTransitionPolicy.allowedTransitions(order.getStatus(), actor).stream()
                .map(Enum::name)
                .toList());
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            summary.setPreviewItem(
                    orderMapper.toOrderItemResponse(order.getItems().getFirst()));
        }
        return summary;
    }

    private OrderDetailResponse toDetail(Order order, Actor actor) {
        OrderDetailResponse detail = orderMapper.toOrderDetailResponse(order);
        detail.setAllowedTransitions(OrderTransitionPolicy.allowedTransitions(order.getStatus(), actor).stream()
                .map(Enum::name)
                .toList());
        detail.setItems(orderMapper.toOrderItemResponses(order.getItems()));
        return detail;
    }

    /**
     * Admin order list with null-safe filters (R-C05-03).
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAdminOrders(AdminOrderSearchRequest request) {
        if (request.getPage() < 0 || request.getSize() < 1) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            parseStatus(request.getStatus());
        }
        if (request.getFromDate() != null
                && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
        int safeSize = Math.clamp(request.getSize(), 1, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                Math.max(request.getPage(), 0), safeSize, Sort.by(CREATE_AT).descending());
        Specification<Order> spec = Specification.where(buildAdminOrderSpec(request));
        return orderRepository.findAll(spec, pageable).map(order -> toSummary(order, Actor.ADMIN));
    }

    private Specification<Order> buildAdminOrderSpec(AdminOrderSearchRequest request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            String search = request.getSearch();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderCode")), pattern),
                        cb.like(cb.lower(root.get("recipientName")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern)));
            }
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                OrderStatus status = parseStatus(request.getStatus());
                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
            }
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(CREATE_AT), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(CREATE_AT), request.getToDate()));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private OrderStatus parseStatus(String raw) {
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    /**
     * Customer cancel: owner + current status PENDING only (R-C05-02).
     * Runs inside one transaction after acquiring the order's pessimistic
     * write lock; stock/voucher/status compensation commits together and the
     * single successful status transition is the exact-once guard
     * (R-C05-04, ADR-003).
     */
    @Transactional
    public OrderResponse cancelOrder(String orderCode, String reason, User user) {
        Order order = orderRepository
                .findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return orderMapper.toOrderResponse(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        compensateCancellation(order);
        applyTransition(
                order, OrderStatus.CANCELLED, Actor.CUSTOMER, user.getId().toString(), normalizeNote(reason));
        orderRepository.save(order);
        orderRepository.flush();
        return orderMapper.toOrderResponse(order);
    }

    /**
     * Admin status transition (R-C05-01/03): calls the same shared
     * transition policy; the controller stays a thin HTTP adapter. Cancelling
     * to CANCELLED runs the same exact-once compensation as customer cancel.
     */
    @Transactional
    public OrderResponse updateOrderStatus(String orderCode, OrderStatus target, String note, User admin) {
        Order order = orderRepository
                .findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.CANCELLED && target == OrderStatus.CANCELLED) {
            compensateCancellation(order);
        }
        applyTransition(order, target, Actor.ADMIN, admin.getId().toString(), normalizeNote(note));
        orderRepository.save(order);
        orderRepository.flush();
        return orderMapper.toOrderResponse(order);
    }

    /**
     * Exact-once cancellation compensation (R-C05-04). Caller has locked the
     * order row and validated the current status is not CANCELLED.
     */
    private void compensateCancellation(Order order) {
        restoreCancellationStock(order);
        reverseCancellationVoucher(order);
    }

    private void restoreCancellationStock(Order order) {
        if (order.getItems() == null) {
            return;
        }
        order.getItems().forEach(item -> restoreCancellationStockItem(order, item));
    }

    private void restoreCancellationStockItem(Order order, OrderItem item) {
        validateCancellationItem(item);
        requireSingleUpdatedRow(productSkuRepository.increaseStock(item.getSku().getId(), item.getQuantity()));

        StockMovement movement = new StockMovement();
        movement.setSku(item.getSku());
        movement.setChangeQty(item.getQuantity());
        movement.setReason("ORDER_CANCEL_RETURN");
        movement.setReferenceId(String.valueOf(order.getId()));
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);
    }

    private void validateCancellationItem(OrderItem item) {
        if (item.getSku() == null || item.getSku().getId() == null || item.getQuantity() <= 0) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private void reverseCancellationVoucher(Order order) {
        // A voucher-bearing order must have exactly one active redemption. A
        // missing/already-reversed ledger row is an integrity failure, not a
        // reason to silently leave the aggregate count unchanged.
        if (!isVoucherBearing(order)) {
            return;
        }
        if (order.getVoucher() == null || order.getVoucher().getId() == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        requireSingleUpdatedRow(voucherRedemptionRepository.reverseRedemptionByOrderId(order.getId()));
        requireSingleUpdatedRow(
                voucherRepository.decreaseUsedCount(order.getVoucher().getId()));
    }

    private boolean isVoucherBearing(Order order) {
        return order.getVoucher() != null
                || (order.getVoucherCode() != null && !order.getVoucherCode().isBlank());
    }

    private void requireSingleUpdatedRow(int updatedRows) {
        if (updatedRows != 1) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

        // Step 2: Normalize selection: deduplicate + sort ascending
        List<Long> selectedSkuIds =
                request.getSelectedSkuIds().stream().distinct().sorted().toList();

        // Step 3: Authoritative recomputation shared with create order (R-C04-03)
        return recomputeCheckout(user, selectedSkuIds, request.getVoucherCode()).response();
    }

    // ────────────────────────────────────────────────────────
    // 2. Create Order
    // ────────────────────────────────────────────────────────
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
        AuthoritativeCheckout authoritative = recomputeCheckout(user, selectedSkuIds, normalizeVoucherCode(request));

        // Step 4: Semantic-compare reviewed snapshot against the authoritative review (R-C04-07)
        CheckoutChangedException mismatch = findMismatch(request, selectedSkuIds, freshCart, authoritative);
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

    private AuthoritativeCheckout recomputeCheckout(User user, List<Long> selectedSkuIds, String voucherCode) {
        Cart cart = getActiveCart(user);
        Map<Long, CartItem> cartItemBySkuId = cart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductSku().getId(), item -> item, (a, b) -> a));

        List<CheckoutItemResponse> items = new ArrayList<>();
        Map<Long, BigDecimal> skuSubtotals = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allLinesSellable = true;
        List<AuthoritativeLine> lines = new ArrayList<>();

        for (Long skuId : selectedSkuIds) {
            CheckoutLineReview lineReview = reviewCheckoutLine(skuId, cartItemBySkuId);
            if (lineReview.issueCode() != null) {
                allLinesSellable = false;
            }
            if (lineReview.lineTotal() != null) {
                subtotal = subtotal.add(lineReview.lineTotal());
                skuSubtotals.merge(skuId, lineReview.lineTotal(), BigDecimal::add);
            }
            items.add(lineReview.item());
            lines.add(lineReview.authoritativeLine());
        }

        // Voucher review: typed result (invalid voucher is an expected review outcome, not an exception)
        VoucherReview voucherReview = evaluateVoucherReview(voucherCode, user, skuSubtotals, subtotal);
        BigDecimal shippingFee = checkoutShippingFee;
        boolean canPlaceOrder = allLinesSellable && voucherReview.applicable();
        BigDecimal totalAmount = calculateCheckoutTotal(subtotal, shippingFee, voucherReview.discountAmount());
        CheckoutResponse response =
                buildCheckoutResponse(items, subtotal, shippingFee, voucherReview, totalAmount, canPlaceOrder);

        return new AuthoritativeCheckout(
                response,
                lines,
                voucherReview.voucher(),
                voucherReview.normalizedCode(),
                subtotal,
                voucherReview.applicable());
    }

    private CheckoutLineReview reviewCheckoutLine(Long skuId, Map<Long, CartItem> cartItemBySkuId) {
        CartItem cartItem = cartItemBySkuId.get(skuId);
        if (cartItem == null) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_IN_CART);
        }

        ProductSku freshSku = null;
        String issueCode;
        if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
            issueCode = ErrorCode.CART_ITEM_QUANTITY_INVALID.name();
        } else {
            freshSku = productSkuRepository.findById(skuId).orElse(null);
            issueCode = resolveCheckoutLineIssue(freshSku, cartItem.getQuantity());
        }

        BigDecimal lineTotal = calculateLineTotal(freshSku, cartItem.getQuantity());
        CheckoutItemResponse item = buildCheckoutItem(skuId, cartItem, freshSku, lineTotal, issueCode);
        BigDecimal unitPrice = freshSku != null ? freshSku.getPrice() : null;
        AuthoritativeLine authoritativeLine =
                new AuthoritativeLine(skuId, cartItem.getQuantity(), unitPrice, lineTotal);
        return new CheckoutLineReview(item, authoritativeLine, lineTotal, issueCode);
    }

    private String resolveCheckoutLineIssue(ProductSku freshSku, int quantity) {
        if (freshSku == null) {
            return ErrorCode.SKU_NOT_FOUND.name();
        }
        if (!freshSku.isActive()) {
            return ErrorCode.PRODUCT_NOT_AVAILABLE.name();
        }
        if (freshSku.getProduct() == null
                || !freshSku.getProduct().isPublished()
                || freshSku.getProduct().isDraft()) {
            return ErrorCode.PRODUCT_NOT_AVAILABLE.name();
        }
        if (freshSku.getStock() == null || freshSku.getStock() < quantity) {
            return ErrorCode.INSUFFICIENT_STOCK.name();
        }
        return null;
    }

    private BigDecimal calculateLineTotal(ProductSku freshSku, Integer quantity) {
        if (freshSku == null || freshSku.getPrice() == null) {
            return null;
        }
        return freshSku.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    private CheckoutItemResponse buildCheckoutItem(
            Long skuId, CartItem cartItem, ProductSku freshSku, BigDecimal lineTotal, String issueCode) {
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
        return itemBuilder.issueCode(issueCode).build();
    }

    private BigDecimal calculateCheckoutTotal(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discountAmount) {
        BigDecimal totalAmount = subtotal.add(shippingFee).subtract(discountAmount);
        return totalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : totalAmount;
    }

    private CheckoutResponse buildCheckoutResponse(
            List<CheckoutItemResponse> items,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            VoucherReview voucherReview,
            BigDecimal totalAmount,
            boolean canPlaceOrder) {
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

    /**
     * Returns a 409 CHECKOUT_CHANGED exception when any order-affecting value in the
     * reviewed snapshot differs from the authoritative review, else null.
     */
    private CheckoutChangedException findMismatch(
            CreateOrderRequest request,
            List<Long> selectedSkuIds,
            Cart freshCart,
            AuthoritativeCheckout authoritative) {
        ReviewedCheckoutRequest reviewedCheckout = request.getReviewedCheckout();
        boolean mismatch = hasSelectedSkuMismatch(reviewedCheckout, selectedSkuIds)
                || hasLineMismatch(reviewedCheckout, freshCart, authoritative)
                || hasVoucherMismatch(reviewedCheckout, authoritative)
                || hasCheckoutOutcomeMismatch(reviewedCheckout, authoritative.response());

        return mismatch ? new CheckoutChangedException(authoritative.response()) : null;
    }

    private boolean hasSelectedSkuMismatch(ReviewedCheckoutRequest reviewedCheckout, List<Long> selectedSkuIds) {
        List<Long> reviewedSkuIds = reviewedCheckout.getItems().stream()
                .map(ReviewedCheckoutItemRequest::getSkuId)
                .distinct()
                .sorted()
                .toList();
        return !reviewedSkuIds.equals(selectedSkuIds);
    }

    private boolean hasLineMismatch(
            ReviewedCheckoutRequest reviewedCheckout, Cart freshCart, AuthoritativeCheckout authoritative) {
        Map<Long, CartItem> cartItemBySkuId = freshCart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductSku().getId(), item -> item, (a, b) -> a));
        Map<Long, AuthoritativeLine> authoritativeLineBySkuId = authoritative.lines().stream()
                .collect(Collectors.toMap(AuthoritativeLine::skuId, line -> line, (a, b) -> a));

        for (ReviewedCheckoutItemRequest reviewedItem : reviewedCheckout.getItems()) {
            CartItem cartItem = cartItemBySkuId.get(reviewedItem.getSkuId());
            AuthoritativeLine line = authoritativeLineBySkuId.get(reviewedItem.getSkuId());
            if (hasLineValueMismatch(reviewedItem, cartItem, line)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLineValueMismatch(
            ReviewedCheckoutItemRequest reviewedItem, CartItem cartItem, AuthoritativeLine line) {
        return cartItem == null
                || line == null
                || different(reviewedItem.getQuantity(), cartItem.getQuantity())
                || different(reviewedItem.getUnitPrice(), line.unitPrice())
                || different(reviewedItem.getLineTotal(), line.lineTotal());
    }

    private boolean hasVoucherMismatch(ReviewedCheckoutRequest reviewedCheckout, AuthoritativeCheckout authoritative) {
        String reviewedCode = reviewedCheckout.getVoucher() != null
                ? normalizeCode(reviewedCheckout.getVoucher().getCode())
                : null;
        if (reviewedCode == null && authoritative.normalizedVoucherCode() == null) {
            return false;
        }

        if (!Objects.equals(reviewedCode, authoritative.normalizedVoucherCode())) {
            return true;
        }

        boolean reviewedApplicable =
                Boolean.TRUE.equals(reviewedCheckout.getVoucher().getApplicable());
        return reviewedApplicable != authoritative.applicable();
    }

    private boolean hasCheckoutOutcomeMismatch(
            ReviewedCheckoutRequest reviewedCheckout, CheckoutResponse authoritativeResponse) {
        return different(reviewedCheckout.getSubtotal(), authoritativeResponse.getSubtotal())
                || different(reviewedCheckout.getEligibleSubtotal(), authoritativeResponse.getEligibleSubtotal())
                || different(reviewedCheckout.getShippingFee(), authoritativeResponse.getShippingFee())
                || different(reviewedCheckout.getDiscountAmount(), authoritativeResponse.getDiscountAmount())
                || different(reviewedCheckout.getTotalAmount(), authoritativeResponse.getTotalAmount())
                || !authoritativeResponse.isCanPlaceOrder();
    }

    private void allocateInventory(AuthoritativeCheckout authoritative, Order order) {
        for (AuthoritativeLine line : authoritative.lines()) {
            ProductSku sku = productSkuRepository
                    .findById(line.skuId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            inventoryService.reserveStock(sku, line.quantity(), order);
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
            AuthoritativeCheckout authoritative) {

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

        for (AuthoritativeLine line : authoritative.lines()) {
            ProductSku sku = productSkuRepository
                    .findById(line.skuId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .sku(sku)
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

    /**
     * Apply the shared transition policy (R-C05-01) to an order within the
     * current transaction. The caller is responsible for holding the order's
     * pessimistic write lock (mutation path) or a read snapshot (read path).
     *
     * <p>Same-target requests complete idempotently without writing a
     * new history entry; terminal-state transitions throw
     * {@link ErrorCode#ORDER_STATUS_CONFLICT}; history is always ordered by
     * timestamp then id.
     */
    private void applyTransition(Order order, OrderStatus target, Actor actor, String actorId, String note) {
        if (order.getStatus() == target) {
            return;
        }
        OrderStatus from = order.getStatus();
        if (OrderTransitionPolicy.isTerminal(from)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        if (!OrderTransitionPolicy.isAllowed(from, target, actor)) {
            throw new AppException(ErrorCode.ORDER_STATUS_CONFLICT);
        }
        order.setStatus(target);
        if (target == OrderStatus.CANCELLED) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        } else if (target == OrderStatus.DELIVERED) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(target.name());
        history.setFromStatus(from.name());
        history.setToStatus(target.name());
        history.setActorType(actor.name());
        history.setActorId(actorId);
        history.setNote(note);
        history.setCreatedAt(OffsetDateTime.now());
        orderStatusHistoryRepository.save(history);
    }

    private VoucherReview evaluateVoucherReview(
            String rawVoucherCode, User user, Map<Long, BigDecimal> skuSubtotals, BigDecimal subtotal) {
        if (rawVoucherCode == null || rawVoucherCode.isBlank()) {
            return new VoucherReview(null, subtotal, BigDecimal.ZERO, true, null, null);
        }

        String normalizedCode = normalizeCode(rawVoucherCode);

        try {
            Voucher voucher = voucherRepository
                    .findByCode(normalizedCode)
                    .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

            BigDecimal eligibleSubtotal = voucherService.calculateEligibleSubtotal(voucher, skuSubtotals, subtotal);
            voucherValidator.validateForCheckout(voucher, user, subtotal, eligibleSubtotal);

            BigDecimal discountAmount = voucherService.getDiscount(voucher, eligibleSubtotal);
            return new VoucherReview(normalizedCode, eligibleSubtotal, discountAmount, true, null, voucher);
        } catch (AppException e) {
            return new VoucherReview(
                    normalizedCode,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    e.getErrorCode().name(),
                    null);
        }
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

    // ════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════

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

    /**
     * Resolve địa chỉ giao hàng:
     * - Có addressId → dùng địa chỉ đã lưu (phải thuộc current user)
     * - Có newAddress → dùng địa chỉ mới (tùy chọn lưu lại)
     * - Đúng một trong hai (XOR)
     */
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

    /**
     * Generate mã đơn hàng unique: ORD-yyyyMMdd-XXXX
     */
    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + datePart + "-" + randomPart;
    }

    private boolean different(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a != b;
        }
        return a.compareTo(b) != 0;
    }

    private boolean different(Integer a, int b) {
        return a == null || a != b;
    }

    private enum OrderStatusHistoryActor {
        CUSTOMER,
        ADMIN
    }

    @FunctionalInterface
    private interface TransactionCallback {
        OrderResponse run();
    }

    private record AuthoritativeCheckout(
            CheckoutResponse response,
            List<AuthoritativeLine> lines,
            Voucher voucher,
            String normalizedVoucherCode,
            BigDecimal subtotal,
            boolean applicable) {}

    private record AuthoritativeLine(Long skuId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    private record CheckoutLineReview(
            CheckoutItemResponse item, AuthoritativeLine authoritativeLine, BigDecimal lineTotal, String issueCode) {}

    private record VoucherReview(
            String normalizedCode,
            BigDecimal eligibleSubtotal,
            BigDecimal discountAmount,
            boolean applicable,
            String issueCode,
            Voucher voucher) {}

    /**
     * Record nội bộ chứa thông tin địa chỉ đã resolve
     */
    private record AddressInfo(UUID addressId, String recipientName, String phone, String fullAddress) {}
}
