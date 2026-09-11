package spring.abtechzone.modules.payment.service;

import java.time.OffsetDateTime;

import spring.abtechzone.modules.order.constant.OrderStatus;

public final class OnlinePaymentRules {
    private OnlinePaymentRules() {}

    public static String reviewReason(
            OrderStatus orderStatus, OffsetDateTime deadline, OffsetDateTime now, boolean hasAppliedPayment) {
        if (hasAppliedPayment) return "EXTRA_PAYMENT";
        if (orderStatus != OrderStatus.PENDING || deadline == null || !now.isBefore(deadline)) return "LATE_SUCCESS";
        return null;
    }
}
