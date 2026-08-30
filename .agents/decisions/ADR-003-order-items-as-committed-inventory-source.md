# ADR-003 — Order Items as the Committed Inventory Source

## Status

Accepted

Accepted by: user, 2026-08-19.

## Context

Creating a COD order immediately decrements SKU stock with an atomic
`stock >= quantity` guard. The persisted `OrderItem` already records which SKU
and quantity the order consumed, while `StockMovement` records the inventory
audit entry. A separate `InventoryReservation` row with status `COMMITTED`
duplicates that information and does not participate in oversell prevention.

The current lifecycle supports full-order cancellation before delivery. It does
not yet require partial fulfillment, per-line cancellation, warehouse
allocation, backorders, or expiring pre-checkout stock holds.

## Decision

- Remove `InventoryReservation`, `InventoryReservationRepository`, and
  `ReservationStatus`.
- Keep the atomic conditional SKU decrement as the oversell guard.
- Treat `OrderItem` as the source of committed SKU quantities for an order.
- Keep `StockMovement` as the inventory audit trail. Order creation writes
  `SALE_OUT`; cancellation will write `ORDER_CANCEL_RETURN`.
- Plan 05 cancellation must lock the order row and use the single successful
  order-status transition as the exact-once guard. Stock restoration and the
  status transition must commit in one transaction.

## Rationale

For full-order COD cancellation, the order row already provides a serialized
lifecycle boundary. Retaining a second per-SKU state machine adds schema,
repository, write, test, and reconciliation cost without adding protection to
the atomic stock decrement.

## Alternatives Considered

### Keep `InventoryReservation` as a committed allocation

This supports per-line release and future warehouse workflows, but duplicates
`OrderItem` in the current scope and makes cancellation maintain two lifecycle
models.

### Rename it to `InventoryAllocation`

The name would be more accurate, but the table remains unnecessary until
partial fulfillment or independent per-line allocation is required.

### Add an `inventoryReleasedAt` flag to `Order`

This makes compensation state explicit but duplicates the guarded order-status
transition for the current full-cancellation policy. It may be introduced later
only if cancellation and stock-release state can legitimately diverge.

## Consequences

### Positive

- Fewer tables, entities, writes, and consistency boundaries.
- Oversell protection remains database-atomic.
- Cancellation can derive restoration quantities directly from immutable order
  items.
- `StockMovement` remains the auditable record of stock changes.

### Negative

- Partial cancellation or fulfillment cannot track release per order line
  independently.
- Future multi-warehouse allocation will require a new allocation model and an
  ADR that supersedes this decision.

## Constraints for Future Changes

- Do not reintroduce a temporary or expiring reservation for committed COD
  orders.
- Do not restore stock from mutable cart or catalog state; use persisted
  `OrderItem` quantities.
- Cancellation must lock the order and update order status, SKU stock, voucher
  compensation, history, and stock movements in one transaction.
- Add a separate allocation model only for an accepted requirement such as
  partial fulfillment, partial cancellation, warehouse allocation, or
  backorder.

## Supersedes

The committed-allocation part of the Plan 04 design; no earlier ADR is
superseded.

## Superseded By

None.
