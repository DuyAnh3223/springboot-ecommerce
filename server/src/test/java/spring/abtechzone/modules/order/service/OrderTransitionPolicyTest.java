package spring.abtechzone.modules.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import spring.abtechzone.modules.order.constant.OrderStatus;
import spring.abtechzone.modules.order.service.OrderTransitionPolicy.Actor;

class OrderTransitionPolicyTest {

    @ParameterizedTest
    @DisplayName("Allowed transitions match the locked matrix (R-C05-01)")
    @CsvSource({
        // Customer owner
        "CUSTOMER, PENDING, CANCELLED, true",
        "CUSTOMER, PENDING, CONFIRMED, false",
        "CUSTOMER, PENDING, SHIPPING, false",
        "CUSTOMER, PENDING, DELIVERED, false",
        "CUSTOMER, PENDING, PENDING, false",
        "CUSTOMER, CONFIRMED, CANCELLED, false",
        // Admin
        "ADMIN, PENDING, CONFIRMED, true",
        "ADMIN, PENDING, CANCELLED, true",
        "ADMIN, PENDING, SHIPPING, false",
        "ADMIN, PENDING, DELIVERED, false",
        "ADMIN, CONFIRMED, SHIPPING, true",
        "ADMIN, CONFIRMED, CANCELLED, true",
        "ADMIN, CONFIRMED, DELIVERED, false",
        "ADMIN, SHIPPING, DELIVERED, true",
        "ADMIN, SHIPPING, CANCELLED, false",
        "ADMIN, SHIPPING, CONFIRMED, false",
        // Terminal states: no transition at all
        "CUSTOMER, DELIVERED, CANCELLED, false",
        "ADMIN, DELIVERED, DELIVERED, false",
        "CUSTOMER, CANCELLED, PENDING, false",
        "ADMIN, CANCELLED, CANCELLED, false",
        "ADMIN, CANCELLED, CONFIRMED, false"
    })
    void transitionMatrix(Actor actor, OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(OrderTransitionPolicy.isAllowed(from, to, actor)).isEqualTo(allowed);
    }

    @Test
    @DisplayName("Customer allowedTransitions from PENDING is exactly CANCELLED")
    void customerPendingAllowedTransitions() {
        assertThat(OrderTransitionPolicy.allowedTransitions(OrderStatus.PENDING, Actor.CUSTOMER))
                .containsExactly(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Admin allowedTransitions reflect the lifecycle per status")
    void adminAllowedTransitions() {
        assertThat(OrderTransitionPolicy.allowedTransitions(OrderStatus.PENDING, Actor.ADMIN))
                .containsExactly(OrderStatus.CANCELLED, OrderStatus.CONFIRMED);
        assertThat(OrderTransitionPolicy.allowedTransitions(OrderStatus.CONFIRMED, Actor.ADMIN))
                .containsExactly(OrderStatus.CANCELLED, OrderStatus.SHIPPING);
        assertThat(OrderTransitionPolicy.allowedTransitions(OrderStatus.SHIPPING, Actor.ADMIN))
                .containsExactly(OrderStatus.DELIVERED);
        assertThat(OrderTransitionPolicy.allowedTransitions(OrderStatus.DELIVERED, Actor.ADMIN))
                .isEmpty();
        assertThat(OrderTransitionPolicy.allowedTransitions(OrderStatus.CANCELLED, Actor.ADMIN))
                .isEmpty();
    }

    @Test
    @DisplayName("Null from/to/actor are never allowed")
    void nullInputsNeverAllowed() {
        assertThat(OrderTransitionPolicy.isAllowed(null, OrderStatus.CANCELLED, Actor.CUSTOMER))
                .isFalse();
        assertThat(OrderTransitionPolicy.isAllowed(OrderStatus.PENDING, null, Actor.CUSTOMER))
                .isFalse();
        assertThat(OrderTransitionPolicy.isAllowed(OrderStatus.PENDING, OrderStatus.CANCELLED, null))
                .isFalse();
    }

    @Test
    @DisplayName("DELIVERED and CANCELLED are terminal")
    void terminalStates() {
        assertThat(OrderTransitionPolicy.isTerminal(OrderStatus.DELIVERED)).isTrue();
        assertThat(OrderTransitionPolicy.isTerminal(OrderStatus.CANCELLED)).isTrue();
        assertThat(List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING))
                .allMatch(status -> !OrderTransitionPolicy.isTerminal(status));
    }
}
