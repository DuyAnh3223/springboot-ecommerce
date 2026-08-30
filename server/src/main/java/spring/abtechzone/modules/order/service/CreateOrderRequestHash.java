package spring.abtechzone.modules.order.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

import spring.abtechzone.modules.order.dto.request.AddressRequest;
import spring.abtechzone.modules.order.dto.request.CreateOrderRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutItemRequest;
import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutRequest;

/**
 * Canonical SHA-256 request hash for idempotency replay detection. The
 * representation is independent of JSON property order and item order, and
 * monetary values are canonicalized so equivalent BigDecimal scales hash equal.
 *
 * <p>Every field is emitted as a length-prefixed value so the concatenation is
 * unambiguous: two different inputs can never produce the same canonical
 * string (e.g. {@code recipientName="A|B", phone="C"} versus
 * {@code recipientName="A", phone="B|C"}). All side-effect fields are hashed,
 * including {@code saveAddress}: saving an address persists a row, so two
 * requests that differ only in {@code saveAddress} must not be treated as the
 * same idempotent request (AC-C04-03).
 */
public final class CreateOrderRequestHash {

    /**
     * Bumped to v2 because the canonical representation changed: length-prefixed
     * fields and the {@code saveAddress} flag. Hashes computed with the v1
     * representation are no longer comparable; any existing {@code request_hash}
     * column must be regenerated (see schema upgrade note).
     */
    private static final String CONTRACT_VERSION = "create-order:v2";

    private static final String PAYMENT_METHOD = "COD";

    private CreateOrderRequestHash() {}

    public static String compute(CreateOrderRequest request, UUID userId) {
        ReviewedCheckoutRequest reviewed = request.getReviewedCheckout();

        StringBuilder sb = new StringBuilder();
        appendField(sb, CONTRACT_VERSION);
        appendField(sb, userId.toString());

        reviewed.getItems().stream()
                .sorted(Comparator.comparing(ReviewedCheckoutItemRequest::getSkuId))
                .forEach(item -> {
                    appendField(sb, item.getSkuId());
                    appendField(sb, item.getQuantity());
                    appendField(sb, canonical(item.getUnitPrice()));
                    appendField(sb, canonical(item.getLineTotal()));
                });

        appendFields(
                sb,
                canonical(reviewed.getSubtotal()),
                canonical(reviewed.getEligibleSubtotal()),
                canonical(reviewed.getShippingFee()),
                canonical(reviewed.getDiscountAmount()),
                canonical(reviewed.getTotalAmount()));

        if (reviewed.getVoucher() != null
                && reviewed.getVoucher().getCode() != null
                && !reviewed.getVoucher().getCode().isBlank()) {
            appendField(sb, normalizeCode(reviewed.getVoucher().getCode()));
            appendField(sb, Boolean.TRUE.equals(reviewed.getVoucher().getApplicable()));
        } else {
            appendField(sb, "-");
            appendField(sb, false);
        }

        if (request.getAddressId() != null) {
            appendField(sb, "id");
            appendField(sb, request.getAddressId());
        } else if (request.getNewUserAddress() != null) {
            AddressRequest address = request.getNewUserAddress();
            appendField(sb, "new");
            appendFields(
                    sb,
                    trim(address.getRecipientName()),
                    trim(address.getPhone()),
                    trim(address.getProvince()),
                    trim(address.getWard()),
                    trim(address.getStreet()));
            // saveAddress persists an Address row (side effect): it must be part of the hash
            appendField(sb, address.isSaveAddress());
        } else {
            appendField(sb, "none");
        }

        appendField(sb, PAYMENT_METHOD);

        return sha256(sb.toString());
    }

    /**
     * Appends a value to the canonical representation with a fixed-width UTF-8
     * length prefix so the concatenation is injective (no delimiter ambiguity).
     */
    private static void appendField(StringBuilder sb, Object value) {
        String text = value == null ? "" : String.valueOf(value);
        sb.append(String.format(Locale.ROOT, "%08d", text.getBytes(StandardCharsets.UTF_8).length));
        sb.append(text);
        sb.append('\n');
    }

    private static void appendFields(StringBuilder sb, Object... values) {
        for (Object value : values) {
            appendField(sb, value);
        }
    }

    private static String canonical(BigDecimal value) {
        if (value == null) {
            return "";
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return getString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @NonNull
    public static String getString(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
