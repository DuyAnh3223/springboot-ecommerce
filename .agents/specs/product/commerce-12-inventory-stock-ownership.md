# SPEC-COMMERCE-12 — Inventory Stock Ownership

## Status

Accepted

Accepted by the user on 2026-09-01. Implementation follows the requirements
and acceptance criteria in this specification; automated verification and
human UAT remain tracked separately.

Amendment A1 accepted by the user on 2026-09-01: preserve the current
disposable development demo dataset through a one-time controlled migration.
This supersedes the clean-reset-only and no-backfill wording for the current
development database in R-C12-07. Staging/production data migration remains
out of scope.

## Context

The current backend declares both `ProductSku.stock` and `Inventory.stock`, but
all active reads and writes use `ProductSku.stock`. The unused Inventory value
can drift and makes ownership ambiguous. `Product.totalStock` is another
persisted copy that is recalculated by admin SKU reconciliation but not by
order placement or cancellation.

The intended boundary is:

```text
ProductSku -> catalog identity and merchandising data
Inventory  -> current on-hand quantity and stock mutations
```

The database is development-only and disposable. There is no Flyway setup;
Hibernate `ddl-auto: update` currently manages the development schema.

## Current Behavior and Evidence

- Cart add/update/merge reads `ProductSku.stock`.
- Public product detail derives SKU and product stock from active Product SKU
  entities.
- Checkout review reloads `ProductSku.stock` as authoritative availability.
- Order placement calls `InventoryService`, but that service atomically updates
  `ProductSku.stock` and writes `SALE_OUT`.
- Cancellation atomically increments `ProductSku.stock` from persisted
  `OrderItem` quantity and writes `ORDER_CANCEL_RETURN`.
- Admin SKU reconciliation writes `ProductSku.stock` and recalculates persisted
  `Product.totalStock`.
- Catalog `inStock` filtering reads persisted `Product.totalStock`.
- The `Inventory` entity has no repository and no active consumer.
- Existing accepted contracts: SPEC-COMMERCE-03, SPEC-COMMERCE-04,
  SPEC-COMMERCE-05, SPEC-COMMERCE-06, SPEC-COMMERCE-11, ADR-003, and ADR-005
  once accepted.
- Documentation baseline: source audit completed on 2026-08-31; no behavior
  tests were run because this task changes only the proposed contract.

## Goal

- Establish Inventory as the single persisted source of current SKU on-hand
  quantity.
- Remove duplicate stock persistence from Product SKU and Product aggregates.
- Preserve current customer/admin API shapes and existing checkout,
  concurrency, transaction, cancellation, and audit behavior.
- Cut over the disposable development database to the new schema while
  preserving the current demo dataset through a controlled one-time migration;
  do not introduce dual writes or a production migration strategy.

## Requirements

### R-C12-01 — Canonical on-hand model

- Every persisted `ProductSku` has exactly one associated `Inventory` row.
- `Inventory.skuId` is both the primary key and a foreign key to
  `ProductSku.id`; Inventory has no independent surrogate ID.
- `Inventory.onHand` is a non-null integer with a database constraint
  `on_hand >= 0`.
- `ProductSku` has no persisted `stock` field or column.
- `Product` has no persisted `totalStock` field or `total_stock` column.
- `StockMovement` remains the audit trail and continues to identify the SKU,
  quantity delta, reason, reference, actor when available, and timestamp.

### R-C12-02 — Inventory-owned reads

- Cart quantity validation reads current on-hand quantity from an
  Inventory-owned service or projection. Cart still does not reserve stock.
- Catalog list/detail and Product responses obtain SKU stock and product total
  stock from Inventory data for active, non-deleted SKUs.
- Catalog `inStock` filtering is true only when at least one active,
  non-deleted SKU has `onHand > 0`; it must not use a persisted Product cache.
- Checkout authoritative recomputation reads Inventory on-hand quantity and
  treats it as the only stock value.
- A missing Inventory row is unavailable to customer/cart/checkout flows and is
  treated as an invariant violation for diagnostics. No code falls back to a
  Product SKU stock value.

### R-C12-03 — Atomic stock mutation and audit

- Inventory owns the conditional decrement used by order creation:

  ```sql
  UPDATE inventory
  SET on_hand = on_hand - :quantity
  WHERE sku_id = :skuId
    AND on_hand >= :quantity;
  ```

- Exactly one updated row means success. Zero updated rows means stock cannot
  be allocated and preserves the existing insufficient-stock behavior.
- The successful decrement and its `SALE_OUT` movement remain in the same
  transaction as order, voucher, and selected-cart mutations.
- Cancellation restores quantity from persisted `OrderItem`, increments
  `Inventory.onHand`, and writes `ORDER_CANCEL_RETURN` in the existing locked
  order transaction.
- Repeated or racing cancellation must not restore on-hand stock twice.
- Distributed locks remain coordination aids; the conditional database update
  remains the final oversell guard.

### R-C12-04 — SKU administration lifecycle

- Existing SKU create/reconcile request fields named `stock` remain accepted
  for API compatibility and represent the desired Inventory on-hand value.
- Creating a SKU creates its Inventory row in the same transaction.
- Updating/reconciling a SKU updates Inventory through the Inventory-owned
  boundary in the same transaction as the SKU change.
- Zero on-hand is valid. Null or negative stock input remains invalid.
- Soft-deleting or deactivating a SKU makes it unavailable to public/cart/
  checkout flows but does not delete order history or stock movements.
- No independent Inventory row can be created for a nonexistent SKU.

### R-C12-05 — Stable external stock contract

- Existing response fields named `stock` and `totalStock` remain unchanged so
  the frontend contract does not change solely because persistence ownership
  changes.
- `stock` is mapped from `Inventory.onHand`.
- `totalStock` is derived as the non-negative sum of `Inventory.onHand` for
  active, non-deleted SKUs represented by the response.
- A raw on-hand change that remains sufficient for the requested checkout
  quantity does not create a false reviewed-checkout mismatch; existing
  Commerce-04 semantic comparison remains authoritative.
- Existing stock-related error codes and HTTP contracts remain unchanged unless
  a later accepted specification explicitly changes them.

### R-C12-06 — Level 1 inventory scope

- Do not add `reserved`, available-to-promise, expiring holds, warehouse
  balances, backorders, partial allocations, or an Inventory controller.
- Do not create another order allocation/reservation table. ADR-003 remains
  authoritative for committed order quantities.
- Do not add asynchronous projection synchronization or a reconciliation job
  for `Product.totalStock`; the duplicate persisted aggregate is removed.

### R-C12-07 — Development schema cutover

- The first implementation targets only a disposable development database.
- Do not dual-write old and new stock columns. For the current demo database,
  run the accepted one-time migration: backfill `inventory.on_hand` from the
  legacy SKU stock, reshape Inventory to shared identity, remove legacy stock
  columns and aggregate trigger dependencies, and verify every SKU has one
  Inventory row.
- Restarting against the old database with `ddl-auto: update` is not accepted
  as schema verification because removed columns may remain. The migration is
  executed explicitly while the backend is stopped and inside a transaction.
- Verify the resulting schema contains `inventory(sku_id, on_hand)` and does
  not contain `product_sku.stock` or `product.total_stock`.
- Flyway adoption and in-place migration for staging/production data are a
  separate future scope that must be completed before non-disposable data is
  introduced.

## Acceptance Criteria

### AC-C12-01 — Clean schema has one stock source

Given a newly recreated development database,
when Hibernate creates the application schema,
then `inventory.on_hand` is the only persisted current stock quantity,
`inventory.sku_id` is unique/primary and references `product_sku.id`, and the
legacy `product_sku.stock` and `product.total_stock` columns do not exist.
Maps to R-C12-01 and R-C12-07.

### AC-C12-02 — SKU creation and reconciliation own Inventory state

Given a valid admin SKU create or reconcile request containing `stock`,
when it commits,
then the Product SKU and exactly one Inventory row commit together and the
requested value is stored as non-negative `onHand`; a failure rolls both back.
Maps to R-C12-01 and R-C12-04.

### AC-C12-03 — Customer reads agree on availability

Given Inventory on-hand changes for an active SKU,
when catalog list/detail, cart validation, and checkout review read that SKU,
then they derive stock from the same Inventory value and no Product/ProductSku
fallback can return a conflicting value.
Maps to R-C12-02 and R-C12-05.

### AC-C12-04 — Product total and in-stock filter are current

Given a product whose active SKUs become sold out or receive returned stock,
when catalog total stock and the `inStock` filter are evaluated,
then both reflect the current Inventory rows without an admin reconciliation or
persisted Product aggregate update.
Maps to R-C12-01, R-C12-02, and R-C12-05.

### AC-C12-05 — Order creation remains atomic and cannot oversell

Given concurrent order attempts whose combined quantity exceeds on-hand stock,
when they execute,
then the conditional Inventory update permits only quantities covered by
on-hand stock, never makes `on_hand` negative, writes one `SALE_OUT` per
successful allocation, and rolls back order/voucher/cart/stock together on a
later failure.
Maps to R-C12-03.

### AC-C12-06 — Cancellation restores exactly once

Given a cancellable order with persisted Order Items,
when customer or admin cancellation succeeds or races with another
cancellation,
then Inventory is restored from Order Item quantities exactly once and the same
transaction writes one `ORDER_CANCEL_RETURN` per restored item and applies the
existing voucher/status/history compensation.
Maps to R-C12-03 and R-C12-06.

### AC-C12-07 — Missing Inventory fails closed

Given a Product SKU with no Inventory row because an invariant was broken,
when catalog/cart/checkout/order allocation evaluates it,
then it is never treated as in stock, order allocation cannot succeed, and no
legacy Catalog stock value is used as fallback.
Maps to R-C12-01 and R-C12-02.

### AC-C12-08 — API stock fields remain compatible

Given existing customer and admin stock request/response contracts,
when the Inventory ownership change is deployed,
then field names and existing stock-related error contracts remain compatible
while their values come from Inventory.
Maps to R-C12-04 and R-C12-05.

### AC-C12-09 — No reservation or duplicate aggregate is introduced

Given the Level 1 implementation,
when the persistence model and runtime flows are inspected,
then there is no `reserved` counter, expiring hold, committed allocation row,
dual-written Catalog stock, or persisted Product total-stock cache.
Maps to R-C12-01 and R-C12-06.

### AC-C12-10 — Development schema cutover is explicit

Given implementation is ready for schema verification and the developer has
authorized the selected dev-only cutover,
when the explicit migration or clean reset is performed,
then the resulting schema is clean, the selected demo-data policy is preserved,
and the operation is not represented as an in-place production migration.
Maps to R-C12-07.

### AC-C12-11 — Demo data is preserved by the controlled migration

Given the current disposable database contains demo Product/SKU rows,
when the Commerce-12 migration runs,
then each existing SKU has exactly one Inventory row whose `on_hand` equals its
legacy stock value before the legacy column is removed, and product/SKU counts
remain unchanged.
Maps to R-C12-01 and R-C12-07.

## Non-Goals

- Flyway dependency, baseline, or versioned production migration.
- Preserving arbitrary staging/production database contents.
- Staging or production database cutover.
- `reserved`, pre-checkout stock holds, reservation expiry, backorders,
  multi-warehouse stock, or partial fulfillment.
- New inventory adjustment/import/admin APIs.
- Changing customer/admin JSON stock field names.
- Changing order idempotency, lock ordering, voucher semantics, cart selection,
  order status transitions, or cancellation eligibility.
- A performance cache or asynchronous projection for product total stock.

## Edge Cases

- SKU creation fails after Product SKU persistence but before Inventory
  persistence; the shared transaction must roll back both.
- Inventory row missing for an existing SKU.
- Null, negative, zero, maximum integer, and overflow-causing quantities.
- Concurrent orders for the last units of one SKU.
- Order failure after stock decrement but before voucher/cart completion.
- Customer and admin cancellation racing for the same order.
- SKU soft-deleted after order creation but before cancellation.
- Active zero-stock SKU versus inactive SKU with positive historical on-hand.
- Product with no active SKUs and product with multiple active SKUs.
- Raw on-hand changes after review while remaining sufficient versus becoming
  insufficient.
- Old development database restarted without being recreated.

## Domain Invariants

- One persisted SKU identity has exactly one current on-hand balance.
- Inventory on-hand is never negative.
- Product and Product SKU do not persist a second stock value.
- API field naming does not determine persistence ownership.
- Cart and product detail do not reserve stock; checkout/order creation remains
  authoritative.
- A database-atomic conditional decrement is the final oversell boundary.
- `OrderItem` is the committed order quantity source; `StockMovement` is the
  inventory audit trail.
- Cancellation derives restoration from persisted Order Items and shares the
  existing transaction/status exact-once boundary.
- Product total stock is derived only from active, non-deleted SKU Inventory
  rows.

## API Contract

No endpoint or JSON field rename is introduced by this specification.

### Request

- Existing admin SKU request field `stock` remains a non-negative integer.
- Existing cart, checkout, order, and cancellation request contracts remain
  unchanged.

### Response

- Existing SKU response field `stock` maps from `Inventory.onHand`.
- Existing product response field `totalStock` is derived from applicable
  Inventory rows.

### Errors

- Existing invalid-stock, insufficient-stock, checkout-changed, system-busy,
  and transaction rollback contracts remain authoritative.
- A missing Inventory row must not disclose internal schema details to the
  client.

## Security / Authorization

- Existing product administration authorization remains unchanged.
- Public catalog stock reads remain anonymous through existing catalog
  endpoints.
- Inventory ownership does not introduce a public Inventory mutation endpoint.
- Client-provided stock is accepted only through existing authorized admin SKU
  workflows; checkout/cart clients cannot set Inventory quantities.

## Data / Persistence Considerations

Conceptual clean schema:

```sql
CREATE TABLE inventory (
    sku_id BIGINT PRIMARY KEY REFERENCES product_sku(id),
    on_hand INTEGER NOT NULL CHECK (on_hand >= 0)
);
```

- The JPA mapping should use shared identity (`@MapsId` or an equivalent mapping
  that produces the same schema invariant).
- Inventory and SKU administration writes share one transaction.
- Order and cancellation mutations preserve their current transaction and lock
  boundaries.
- `stock_movement` continues to reference SKU identity and is not a second
  balance source.
- The current demo database uses a one-time explicit migration script; it is
  not auto-run by Hibernate and is not a Flyway production migration.
- Before staging or production, replace this dev-only script with an accepted
  versioned migration strategy and a separately reviewed data policy.

## Verification Strategy

### Unit

- Inventory service tests for non-negative validation, guarded decrement,
  increment, missing row, and movement creation.
- Product/SKU service tests proving admin request `stock` maps to Inventory and
  transaction failures do not leave a partial pair.
- Cart and Checkout tests proving availability comes from Inventory.
- Catalog mapping/filter tests proving total stock and in-stock behavior use
  current Inventory values.
- Cancellation tests proving persisted Order Item quantities restore Inventory
  exactly once.

### MVC / Component

- Existing product/catalog/cart/checkout/order controller contracts retain
  their JSON field names and error envelopes.
- No new public Inventory endpoint is exposed.

### Integration

- Clean-schema PostgreSQL test asserts Inventory constraints and absence of the
  two legacy stock columns.
- Concurrent order test proves no oversell through the Inventory conditional
  update.
- Transaction rollback test fails after stock decrement and proves Inventory,
  movement, order, voucher, and cart all roll back.
- Cancellation integration tests prove one restoration after normal, repeated,
  and concurrent cancellation, including a soft-deleted SKU.
- Catalog integration test proves sale/cancellation changes total stock and
  in-stock filtering without admin reconciliation.

### Static / Build / Runtime

- Compile and run focused Product, Inventory, Cart, Catalog, Checkout, and Order
  suites.
- Run formatting/static checks required by the server module.
- Run GitNexus impact before implementation edits and change detection before
  any commit.
- Inspect the recreated PostgreSQL schema rather than inferring column removal
  from JPA entities.

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
| --- | --- | --- | --- |
| AC-C12-01 | clean-schema PostgreSQL assertion | Inspect clean dev schema | PLANNED |
| AC-C12-02 | SKU/Inventory transaction integration test | Create and edit SKU in admin | PLANNED |
| AC-C12-03 | Cart/Catalog/Checkout focused tests | Compare stock across PDP, cart, checkout | PLANNED |
| AC-C12-04 | catalog aggregate/filter integration test | Sell out and cancel, then refresh catalog | PLANNED |
| AC-C12-05 | concurrency and rollback integration tests | Place orders for limited stock | PLANNED |
| AC-C12-06 | cancellation unit/integration tests | Cancel once and retry cancellation | PLANNED |
| AC-C12-07 | missing-row focused tests | Not required for normal UAT | PLANNED |
| AC-C12-08 | MVC/serialization compatibility tests | Exercise existing frontend flows | PLANNED |
| AC-C12-09 | schema/source audit | Inspect no reservation/duplicate counter | PLANNED |
| AC-C12-10 | clean database bootstrap check | Authorize reset and verify seed data | PLANNED |

## Acceptance Scenarios

1. Recreate the development database, start the backend, and verify every
   seeded SKU has one Inventory row with non-negative on-hand stock.
2. Create and edit a SKU through the existing admin workflow; confirm existing
   `stock` fields still work while only Inventory persists the balance.
3. View an in-stock SKU in catalog detail, add it to cart, review checkout, and
   confirm every surface reports the same Inventory-derived quantity.
4. Place an order for the last units and confirm the product leaves the
   in-stock catalog filter without an admin reconcile operation.
5. Cancel that order and confirm the product returns to the in-stock filter,
   stock is restored once, and an `ORDER_CANCEL_RETURN` movement exists.
6. Race orders beyond available quantity and confirm no negative stock or
   oversell.
7. Force a failure after decrement and confirm order, voucher, cart, Inventory,
   and movement mutations roll back together.

Acceptance status: `UAT PENDING`.

## Open Questions

None for the proposed Level 1 boundary. Acceptance of this specification and
ADR-005 is required before implementation. Flyway, persistent-data migration,
reserved stock, warehouse allocation, or a performance projection require a
separate accepted scope.
