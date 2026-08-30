package spring.abtechzone.modules.order.service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import spring.abtechzone.modules.order.constant.OrderStatus;

/**
 * Shared order transition policy (R-C05-01). Single source of truth for which
 * actor may move an order from one status to another and what the COD payment
 * status becomes. Controllers must not duplicate this logic.
 */
public final class OrderTransitionPolicy {

    /** Actor that operates on the order through the customer APIs. */
    public enum Actor {
        CUSTOMER,
        ADMIN
    }

    private static final Map<Actor, Map<OrderStatus, Set<OrderStatus>>> ALLOWED = new EnumMap<>(Actor.class);

    static {
        // Customer owner: PENDING -> CANCELLED only
        ALLOWED.put(Actor.CUSTOMER, new EnumMap<>(OrderStatus.class));
        ALLOWED.get(Actor.CUSTOMER).put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CANCELLED));

        // Admin: PENDING -> CONFIRMED|CANCELLED, CONFIRMED -> SHIPPING|CANCELLED,
        // SHIPPING -> DELIVERED. DELIVERED/CANCELLED are terminal.
        ALLOWED.put(Actor.ADMIN, new EnumMap<>(OrderStatus.class));
        ALLOWED.get(Actor.ADMIN).put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED.get(Actor.ADMIN).put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        ALLOWED.get(Actor.ADMIN).put(OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED));
    }

    private OrderTransitionPolicy() {}

    public static boolean isAllowed(OrderStatus from, OrderStatus to, Actor actor) {
        Map<OrderStatus, Set<OrderStatus>> byActor = ALLOWED.get(actor);
        if (byActor == null || from == null || to == null) {
            return false;
        }
        Set<OrderStatus> targets = byActor.get(from);
        return targets != null && targets.contains(to);
    }

    public static List<OrderStatus> allowedTransitions(OrderStatus from, Actor actor) {
        Map<OrderStatus, Set<OrderStatus>> byActor = ALLOWED.get(actor);
        if (byActor == null || from == null) {
            return List.of();
        }
        Set<OrderStatus> targets = byActor.get(from);
        if (targets == null) {
            return List.of();
        }
        return targets.stream().sorted(Comparator.comparing(Enum::name)).toList();
    }

    public static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED;
    }
}
