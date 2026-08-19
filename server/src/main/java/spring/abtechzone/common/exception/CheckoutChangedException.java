package spring.abtechzone.common.exception;

import spring.abtechzone.modules.order.dto.response.CheckoutResponse;

/**
 * Carries the latest authoritative checkout review so the 409 CHECKOUT_CHANGED
 * response can include it in ApiResult.result.
 */
public class CheckoutChangedException extends AppException {

    private final CheckoutResponse latestReview;

    public CheckoutChangedException(CheckoutResponse latestReview) {
        super(ErrorCode.CHECKOUT_CHANGED);
        this.latestReview = latestReview;
    }

    public CheckoutResponse getLatestReview() {
        return latestReview;
    }
}
