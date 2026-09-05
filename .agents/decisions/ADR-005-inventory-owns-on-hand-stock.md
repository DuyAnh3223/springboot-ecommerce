# ADR-005 — Inventory Owns On-Hand Stock

## Status

Accepted

Accepted by the user on 2026-09-01. This decision constrains the Commerce-12
implementation; production readiness and human UAT remain tracked separately.

Amendment A1 accepted by the user on 2026-09-01: the current disposable
development database will use a one-time controlled migration to preserve its
280 demo Product/SKU rows. This replaces the original clean-reset-only choice
for this database. It does not authorize a staging/production migration or
Flyway adoption.

## Context

The current model declares stock in two places:

- `ProductSku.stock` in the Product module; and
- `Inventory.stock` in the Inventory module.

Only `ProductSku.stock` participates in current behavior. Cart validation,
catalog responses, checkout review, the atomic order decrement, cancellation
compensation, and product aggregates all read or mutate the Product SKU value.
The `Inventory` entity has no repository or active read/write path, so its value
can drift without affecting runtime behavior.

`Product.totalStock` is also persisted as a derived aggregate. It is recalculated
during admin SKU reconciliation but is not updated by order placement or
cancellation. Catalog filtering can therefore observe a different availability
state from product detail and checkout.

The Inventory module is intended to own stock behavior and stock movements. A
catalog entity retaining the authoritative stock value makes that boundary
misleading and permits future code to create multiple sources of truth.

The database is currently development-only and disposable. The project does
not yet use Flyway; development schema management uses Hibernate
`ddl-auto: update`.

## Decision

- The Inventory module owns current physical on-hand stock.
- Persist exactly one `Inventory` row per persisted `ProductSku` using the SKU
  identifier as the Inventory primary key and foreign key.
- Name the authoritative persisted quantity `Inventory.onHand`. It must be
  non-null and non-negative.
- Remove persisted stock from `ProductSku`. Product SKU remains the catalog
  identity and owns SKU code, price, attributes, images, activation, product
  relationship, and lifecycle metadata.
- Remove persisted `Product.totalStock`. Product-level total stock is derived
  from `Inventory.onHand` for active, non-deleted SKUs through an Inventory-owned
  query or projection.
- Preserve the existing external API field names `stock` and `totalStock` for
  compatibility. They are response/request contract names, not persistence
  ownership indicators.
- Inventory repositories and services own all stock reads and mutations.
  Catalog, Product, Cart, Checkout, and Order code may consume Inventory-owned
  services or read projections but must not persist another stock counter.
- Order placement keeps a database-atomic conditional decrement guarded by
  `on_hand >= quantity`. Cancellation restores on-hand stock in the same locked
  order transaction. `SALE_OUT` and `ORDER_CANCEL_RETURN` remain the stock audit
  movements.
- Creating or reconciling a SKU and creating or updating its Inventory row occur
  in one transaction. Soft-deleting a SKU does not delete historical stock
  movements or order evidence.
- Level 1 does not add `reserved`, pre-checkout holds, expiration, warehouse
  allocation, or another allocation state machine.
- Because the database is development-only, the first implementation uses an
  explicit dev-only migration to backfill Inventory from the legacy SKU stock,
  preserve the demo rows, and then remove obsolete columns and trigger
  dependencies. It does not dual-write, and it does not introduce Flyway as a
  production migration strategy.

## Rationale

One module and one persisted value must answer the question "how many units are
currently on hand for this SKU?" Moving that value to Inventory makes the
existing module name, stock movement audit, and mutation rules agree.

A shared primary key expresses the one-to-one relationship without an
unnecessary second surrogate identifier. Keeping API field names stable avoids
coupling a persistence refactor to a frontend contract migration.

Removing `Product.totalStock` avoids a second mutable copy that every sale,
cancellation, adjustment, import, and future warehouse operation would have to
synchronize. A derived query is sufficient for the current Level 1 scale; a
dedicated projection may be introduced later with explicit consistency and
reconciliation rules if measured performance requires it.

A clean rebuild is appropriate only because the current database is disposable
development state. Hibernate update is not treated as evidence that removed
columns were dropped.

## Alternatives Considered

### Keep `ProductSku.stock` and remove the unused `Inventory` entity

This is the smallest Level 1 model and retains the existing atomic queries. It
was rejected because the intended durable module boundary is that Inventory,
not Catalog, owns stock and stock mutations.

### Keep both stock columns synchronized

This was rejected because dual writes create partial-failure and reconciliation
paths while still leaving readers able to choose different values.

### Keep `Product.totalStock` as a maintained cache

This can optimize product listing and sorting, but it requires every stock
mutation to update the aggregate atomically and still needs reconciliation. It
is deferred until performance evidence justifies a formal read projection.

### Add `onHand` and `reserved` now

This supports pre-checkout holds and warehouse allocation, but those behaviors
are outside the accepted Level 1 COD flow. Adding `reserved` without a lifecycle
that owns it would create another ambiguous counter.

### Introduce Flyway before this refactor

Flyway is required before persistent staging or production data needs
versioned, in-place migration. It is not required to replace a disposable
development schema and is therefore outside this decision's implementation
scope.

## Consequences

### Positive

- One persisted source of truth for current SKU availability.
- Inventory has a real ownership boundary rather than only an application
  service name.
- Atomic oversell protection and stock movement auditing remain intact.
- Catalog list, product detail, cart, checkout, and cancellation can no longer
  disagree because of separately persisted stock counters.
- Existing frontend/API stock field names can remain unchanged.

### Negative

- Catalog and Product queries need an Inventory join, projection, or aggregate
  query.
- SKU administration must coordinate Catalog identity and Inventory state in
  one transaction.
- The development cutover requires a controlled, transactional migration while
  the backend is stopped; a backup and post-migration schema/data checks are
  required.
- Existing tests and fixtures that construct `ProductSku.stock` or query the
  `product_sku.stock` column must be rewritten.
- A later move to persistent environments still requires a separate Flyway
  adoption and migration decision or plan.

## Constraints for Future Changes

- Do not reintroduce a persisted stock counter on `ProductSku`, `Product`, or a
  client-side store.
- Do not dual-write stock to Catalog and Inventory.
- Treat a missing Inventory row as unavailable and an invariant violation; do
  not silently fall back to a Catalog value.
- Preserve database-atomic decrement as the final oversell boundary even when
  distributed locks are held.
- Preserve ADR-003: committed order quantity comes from persisted `OrderItem`,
  and no expiring committed-order reservation is introduced.
- Add `reserved`, warehouse balances, or an allocation model only through an
  accepted requirement and an ADR that defines its lifecycle and availability
  formula.
- Keep this migration dev-only. Introduce an accepted versioned migration
  strategy before staging/production contains data that cannot be discarded.

## Relationship to Existing Decisions

This decision complements ADR-003. ADR-003 defines the source of committed
order quantities and cancellation evidence; ADR-005 defines the source of
current on-hand SKU availability.

## Supersedes

None.

## Superseded By

None.
