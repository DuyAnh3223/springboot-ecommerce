package spring.abtechzone.modules.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.cart.dto.request.CartMergeItemRequest;
import spring.abtechzone.modules.cart.dto.request.CartMergeRequest;
import spring.abtechzone.modules.cart.service.CartMergeRequestNormalizer;

class CartMergeRequestNormalizerTest {

    private static final UUID MERGE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Test
    void duplicateSkusAreAggregatedAndHashIsIndependentOfInputOrder() {
        CartMergeRequest first = request(
                CartMergeItemRequest.builder().skuId(42L).quantity(1).build(),
                CartMergeItemRequest.builder().skuId(17L).quantity(2).build(),
                CartMergeItemRequest.builder().skuId(17L).quantity(3).build());
        CartMergeRequest second = request(
                CartMergeItemRequest.builder().skuId(17L).quantity(3).build(),
                CartMergeItemRequest.builder().skuId(17L).quantity(2).build(),
                CartMergeItemRequest.builder().skuId(42L).quantity(1).build());

        var normalizedFirst = CartMergeRequestNormalizer.normalize(first);
        var normalizedSecond = CartMergeRequestNormalizer.normalize(second);

        assertThat(normalizedFirst.items())
                .extracting(CartMergeRequestNormalizer.NormalizedCartMergeItem::skuId)
                .containsExactly(17L, 42L);
        assertThat(normalizedFirst.items().get(0).quantity()).isEqualTo(5);
        assertThat(normalizedFirst.requestHash()).isEqualTo(normalizedSecond.requestHash());
    }

    @Test
    void duplicateQuantityOverflowIsRejectedBeforeBusinessProcessing() {
        CartMergeRequest request = request(
                CartMergeItemRequest.builder()
                        .skuId(17L)
                        .quantity(Integer.MAX_VALUE)
                        .build(),
                CartMergeItemRequest.builder().skuId(17L).quantity(1).build());

        assertThatThrownBy(() -> CartMergeRequestNormalizer.normalize(request))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_KEY));
    }

    private CartMergeRequest request(CartMergeItemRequest... items) {
        return CartMergeRequest.builder()
                .mergeId(MERGE_ID)
                .items(List.of(items))
                .build();
    }
}
