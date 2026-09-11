package spring.abtechzone.modules.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.payment.service.OnlinePaymentRules;

class OnlinePaymentRulesTest {
    private final OffsetDateTime now = OffsetDateTime.parse("2026-09-06T08:00:00Z");

    @Test
    void pendingBeforeDeadlineCanApply() {
        assertThat(OnlinePaymentRules.reviewReason(OrderStatus.PENDING, now.plusSeconds(1), now, false))
                .isNull();
    }

    @Test
    void exactDeadlineIsLateEvenBeforeExpiryJobRuns() {
        assertThat(OnlinePaymentRules.reviewReason(OrderStatus.PENDING, now, now, false))
                .isEqualTo("LATE_SUCCESS");
    }

    @Test
    void cancelledOrderNeverReopens() {
        assertThat(OnlinePaymentRules.reviewReason(OrderStatus.CANCELLED, now.plusMinutes(10), now, false))
                .isEqualTo("LATE_SUCCESS");
    }

    @Test
    void secondChargeRequiresReview() {
        assertThat(OnlinePaymentRules.reviewReason(OrderStatus.CONFIRMED, now.plusMinutes(10), now, true))
                .isEqualTo("EXTRA_PAYMENT");
    }

    @Test
    void missingDeadlineFailsClosed() {
        assertThat(OnlinePaymentRules.reviewReason(OrderStatus.PENDING, null, now, false))
                .isEqualTo("LATE_SUCCESS");
    }
}
