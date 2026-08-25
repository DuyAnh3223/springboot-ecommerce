package spring.abtechzone.modules.cart.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.dto.request.CartMergeItemRequest;
import spring.abtechzone.modules.cart.dto.request.CartMergeRequest;

public final class CartMergeRequestNormalizer {

    private static final String CONTRACT_VERSION = "C06-v1";
    private static final int MAX_BATCH_ITEMS = 100;

    private CartMergeRequestNormalizer() {}

    public static NormalizedCartMerge normalize(CartMergeRequest request) {
        if (request == null
                || request.getMergeId() == null
                || request.getItems() == null
                || request.getItems().isEmpty()
                || request.getItems().size() > MAX_BATCH_ITEMS) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        Map<Long, Long> quantitiesBySku = new TreeMap<>();
        for (CartMergeItemRequest item : request.getItems()) {
            if (item == null
                    || item.getSkuId() == null
                    || item.getSkuId() <= 0
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }

            long current = quantitiesBySku.getOrDefault(item.getSkuId(), 0L);
            final long aggregated;
            try {
                aggregated = Math.addExact(current, item.getQuantity().longValue());
            } catch (ArithmeticException exception) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            if (aggregated > Integer.MAX_VALUE) {
                throw new AppException(ErrorCode.INVALID_KEY);
            }
            quantitiesBySku.put(item.getSkuId(), aggregated);
        }

        var items = new ArrayList<NormalizedCartMergeItem>(quantitiesBySku.size());
        StringBuilder canonical = new StringBuilder(CONTRACT_VERSION);
        quantitiesBySku.forEach((skuId, quantity) -> {
            int normalizedQuantity = Math.toIntExact(quantity);
            items.add(new NormalizedCartMergeItem(skuId, normalizedQuantity));
            canonical.append("|skuId=").append(skuId).append(":quantity=").append(normalizedQuantity);
        });

        return new NormalizedCartMerge(request.getMergeId(), items, sha256(canonical.toString()));
    }

    private static String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16)).append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record NormalizedCartMerge(UUID mergeId, ArrayList<NormalizedCartMergeItem> items, String requestHash) {}

    public record NormalizedCartMergeItem(Long skuId, Integer quantity) {}
}
