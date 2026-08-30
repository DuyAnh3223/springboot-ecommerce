package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutItemRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutItemResponse;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.VoucherReviewResponse;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.service.VoucherService;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckoutService {

    UserRepository userRepository;
    CartRepository cartRepository;
    VoucherRepository voucherRepository;
    ProductSkuRepository productSkuRepository;
    VoucherValidator voucherValidator;
    AuthService authService;
    VoucherService voucherService;

    @Value("${app.checkout.shipping-fee:30000}")
    BigDecimal checkoutShippingFee = BigDecimal.valueOf(30000);

    @Transactional(readOnly = true)
    public CheckoutResponse checkoutReview(CheckoutRequest request) {
        User user = getAuthenticatedUser();
        if (request.getSelectedSkuIds() == null
                || request.getSelectedSkuIds().isEmpty()
                || request.getSelectedSkuIds().stream().anyMatch(id -> id == null || id <= 0)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        List<Long> selectedSkuIds =
                request.getSelectedSkuIds().stream().distinct().sorted().toList();
        return recomputeCheckout(user, selectedSkuIds, request.getVoucherCode()).response();
    }

    AuthoritativeCheckout recomputeCheckout(User user, List<Long> selectedSkuIds, String voucherCode) {
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

    CheckoutChangedException findMismatch(
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

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
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

    record AuthoritativeCheckout(
            CheckoutResponse response,
            List<AuthoritativeLine> lines,
            Voucher voucher,
            String normalizedVoucherCode,
            BigDecimal subtotal,
            boolean applicable) {}

    record AuthoritativeLine(Long skuId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}

    private record CheckoutLineReview(
            CheckoutItemResponse item, AuthoritativeLine authoritativeLine, BigDecimal lineTotal, String issueCode) {}

    private record VoucherReview(
            String normalizedCode,
            BigDecimal eligibleSubtotal,
            BigDecimal discountAmount,
            boolean applicable,
            String issueCode,
            Voucher voucher) {}
}
