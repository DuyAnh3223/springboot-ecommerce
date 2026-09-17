package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.dto.request.CheckoutRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutItemRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutRequest;
import spring.abtechzone.modules.order.dto.response.CheckoutItemResponse;
import spring.abtechzone.modules.order.dto.response.CheckoutResponse;
import spring.abtechzone.modules.order.dto.response.VoucherReviewResponse;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.shipment.dto.GhnFeeResult;
import spring.abtechzone.modules.shipment.dto.ResolvedShippingRoute;
import spring.abtechzone.modules.shipment.dto.ShippingAddressData;
import spring.abtechzone.modules.shipment.dto.ShippingAddressSnapshot;
import spring.abtechzone.modules.shipment.service.ShippingFeeService;
import spring.abtechzone.modules.shipment.service.ShippingLocationService;
import spring.abtechzone.modules.user.entity.Address;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.AddressRepository;
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
    InventoryService inventoryService;
    AddressRepository addressRepository;
    ShippingFeeService shippingFeeService;
    ShippingLocationService shippingLocationService;

    // Used only by Mockito tests that instantiate this service without the GHN bean.
    // Spring production wiring always takes the ShippingFeeService branch below.
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
        ShippingAddressData shippingAddress =
                resolveShippingAddress(user, request.getAddressId(), request.getNewUserAddress());
        return recomputeCheckout(user, selectedSkuIds, request.getVoucherCode(), shippingAddress)
                .response();
    }

    AuthoritativeCheckout recomputeCheckout(User user, List<Long> selectedSkuIds, String voucherCode) {
        return recomputeCheckout(user, selectedSkuIds, voucherCode, null);
    }

    AuthoritativeCheckout recomputeCheckout(
            User user, List<Long> selectedSkuIds, String voucherCode, ShippingAddressData shippingAddress) {
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
        BigDecimal shippingFee = resolveShippingFee(shippingAddress);
        boolean shippingAvailable = shippingFee != null;
        boolean canPlaceOrder = allLinesSellable && voucherReview.applicable() && shippingAvailable;
        BigDecimal totalAmount = calculateCheckoutTotal(subtotal, shippingFee, voucherReview.discountAmount());
        CheckoutResponse response = buildCheckoutResponse(
                items, subtotal, shippingFee, voucherReview, totalAmount, canPlaceOrder, shippingAddress);

        return new AuthoritativeCheckout(
                response,
                lines,
                voucherReview.voucher(),
                voucherReview.normalizedCode(),
                subtotal,
                voucherReview.applicable());
    }

    private BigDecimal resolveShippingFee(ShippingAddressData shippingAddress) {
        // The null dependency exists only for Mockito unit tests that exercise the
        // pre-shipment checkout contract. A Spring application always wires GHN.
        if (shippingFeeService == null) {
            return checkoutShippingFee;
        }
        if (shippingAddress == null) {
            throw new AppException(ErrorCode.ADDRESS_REQUIRED);
        }
        GhnFeeResult result = shippingFeeService.calculate(shippingAddress);
        return result.totalFee();
    }

    private ShippingAddressData resolveShippingAddress(
            User user,
            java.util.UUID addressId,
            spring.abtechzone.modules.order.dto.request.AddressRequest newAddress) {
        if (shippingFeeService == null && addressId == null && newAddress == null) {
            return null;
        }
        if ((addressId == null) == (newAddress == null)) {
            throw new AppException(ErrorCode.ADDRESS_REQUIRED);
        }
        if (addressId != null) {
            if (addressRepository == null) {
                throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
            }
            Address address = addressRepository
                    .findById(addressId)
                    .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
            if (address.getUser() == null || !address.getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.ADDRESS_NOT_BELONG_TO_USER);
            }
            return canonicalizeShippingAddress(new ShippingAddressData(
                    address.getId(),
                    address.getRecipientName(),
                    address.getPhone(),
                    address.getProvince(),
                    address.getDistrict(),
                    address.getWard(),
                    address.getStreet(),
                    address.getGhnProvinceId(),
                    address.getGhnDistrictId(),
                    address.getGhnWardCode()));
        }
        return canonicalizeShippingAddress(new ShippingAddressData(
                null,
                newAddress.getRecipientName(),
                newAddress.getPhone(),
                newAddress.getProvince(),
                newAddress.getDistrict(),
                newAddress.getWard(),
                newAddress.getStreet(),
                newAddress.getGhnProvinceId(),
                newAddress.getGhnDistrictId(),
                newAddress.getGhnWardCode()));
    }

    private ShippingAddressData canonicalizeShippingAddress(ShippingAddressData address) {
        if (shippingLocationService == null) {
            return address;
        }
        if (address.provinceId() == null || address.districtId() == null || address.wardCode() == null) {
            throw new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID);
        }
        ResolvedShippingRoute route =
                shippingLocationService.resolveRoute(address.provinceId(), address.districtId(), address.wardCode());
        return new ShippingAddressData(
                address.addressId(),
                address.recipientName(),
                address.phone(),
                route.province(),
                route.district(),
                route.ward(),
                address.street(),
                route.provinceId(),
                route.districtId(),
                route.wardCode());
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
                || hasShippingAddressMismatch(reviewedCheckout, authoritative.response())
                || hasCheckoutOutcomeMismatch(reviewedCheckout, authoritative.response());

        return mismatch ? new CheckoutChangedException(authoritative.response()) : null;
    }

    private CheckoutLineReview reviewCheckoutLine(Long skuId, Map<Long, CartItem> cartItemBySkuId) {
        CartItem cartItem = cartItemBySkuId.get(skuId);
        if (cartItem == null) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_IN_CART);
        }

        ProductSku freshSku = null;
        int onHand = 0;
        String issueCode;
        if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
            issueCode = ErrorCode.CART_ITEM_QUANTITY_INVALID.name();
        } else {
            freshSku = productSkuRepository.findById(skuId).orElse(null);
            onHand = freshSku == null ? 0 : inventoryService.getOnHandOrZero(skuId);
            issueCode = resolveCheckoutLineIssue(freshSku, cartItem.getQuantity(), onHand);
        }

        BigDecimal lineTotal = calculateLineTotal(freshSku, cartItem.getQuantity());
        CheckoutItemResponse item = buildCheckoutItem(skuId, cartItem, freshSku, onHand, lineTotal, issueCode);
        BigDecimal unitPrice = freshSku != null ? freshSku.getPrice() : null;
        AuthoritativeLine authoritativeLine =
                new AuthoritativeLine(skuId, cartItem.getQuantity(), unitPrice, lineTotal);
        return new CheckoutLineReview(item, authoritativeLine, lineTotal, issueCode);
    }

    private String resolveCheckoutLineIssue(ProductSku freshSku, int quantity, int onHand) {
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
        if (onHand < quantity) {
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
            Long skuId, CartItem cartItem, ProductSku freshSku, int onHand, BigDecimal lineTotal, String issueCode) {
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
                    .availableStock(onHand);
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
            boolean canPlaceOrder,
            ShippingAddressData shippingAddress) {
        return CheckoutResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .eligibleSubtotal(voucherReview.eligibleSubtotal())
                .shippingFee(shippingFee)
                .discountAmount(voucherReview.discountAmount())
                .totalAmount(totalAmount)
                .shippingAddress(toShippingAddressSnapshot(shippingAddress))
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

    private ShippingAddressSnapshot toShippingAddressSnapshot(ShippingAddressData address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddressSnapshot(
                address.addressId(),
                address.recipientName(),
                address.phone(),
                address.province(),
                address.district(),
                address.ward(),
                address.street(),
                address.provinceId(),
                address.districtId(),
                address.wardCode());
    }

    private boolean hasShippingAddressMismatch(
            ReviewedCheckoutRequest reviewedCheckout, CheckoutResponse authoritativeResponse) {
        ShippingAddressSnapshot expected = authoritativeResponse.getShippingAddress();
        if (expected == null) {
            return false;
        }
        ShippingAddressSnapshot actual = reviewedCheckout.getShippingAddress();
        return actual == null
                || !Objects.equals(actual.addressId(), expected.addressId())
                || !Objects.equals(trim(actual.recipientName()), trim(expected.recipientName()))
                || !Objects.equals(trim(actual.phone()), trim(expected.phone()))
                || !Objects.equals(trim(actual.province()), trim(expected.province()))
                || !Objects.equals(trim(actual.district()), trim(expected.district()))
                || !Objects.equals(trim(actual.ward()), trim(expected.ward()))
                || !Objects.equals(trim(actual.street()), trim(expected.street()))
                || !Objects.equals(actual.ghnProvinceId(), expected.ghnProvinceId())
                || !Objects.equals(actual.ghnDistrictId(), expected.ghnDistrictId())
                || !Objects.equals(trim(actual.ghnWardCode()), trim(expected.ghnWardCode()));
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

    private String trim(String value) {
        return value == null ? null : value.trim();
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
