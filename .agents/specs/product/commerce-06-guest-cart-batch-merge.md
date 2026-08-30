# Commerce-06 — Idempotent Guest-Cart Batch Merge

## Status and authority

Accepted for implementation by the explicit implementation request for Plan 06.
This specification fills the material gaps identified by the plan; it does not
expand the scope into checkout or stock reservation.

## Goal

After authentication, merge the browser's guest-cart entries through one
authenticated batch request. A retry of the same attempt must not add quantity
twice, a business rejection must not discard other valid entries, and rejected
guest entries must remain locally available with a Vietnamese explanation.

## Requirements

### R-C06-01 — Normalized batch contract

- `POST /cart/merge` requires an authenticated user.
- The request contains a UUID `mergeId` and 1–100 items.
- Each item has a positive `skuId` and positive integer `quantity`.
- Duplicate SKU rows are aggregated with checked `long` addition, then must fit
  the cart quantity domain (`1..Integer.MAX_VALUE`).
- Processing and hashing use ascending SKU ID order. The canonical hash is
  SHA-256 of `C06-v1|skuId=...:quantity=...` tuples joined by `|`; `mergeId` is
  intentionally excluded from the hash.

### R-C06-02 — Durable idempotency and concurrency

- Redis is checked before any database mutation using
  `cart-merge:<userId>:<mergeId>` and a 24-hour TTL.
- The cached value is an internal envelope containing the canonical request hash
  and the typed response; the public response does not expose the hash.
- A Redis read failure returns `503 SYSTEM_BUSY` and does not mutate the cart.
- A per-user distributed lock (`lock:user-cart:<userId>`) is acquired with the
  Redisson watchdog overload. Redis is rechecked after acquiring the lock.
- A durable ledger row is written in the same database transaction as valid cart
  mutations. It has a unique `(user_id, merge_id)` key, canonical request hash,
  serialized typed response, and creation timestamp.
- Same `mergeId` + same hash replays the stored result. Same `mergeId` + a
  different hash returns `409 MERGE_ID_REUSED` without mutation.
- A post-commit Redis write failure is logged and does not undo the committed
  ledger/cart transaction; a later retry replays the ledger.

### R-C06-03 — Partial business result

Each normalized item produces exactly one result:

- `MERGED`: valid item was added to, or accumulated in, the active cart;
  `mergedQuantity` is the resulting cart quantity.
- `REJECTED`: no quantity was added for that item and `mergedQuantity` is `0`.

The stable rejection reason codes are `SKU_NOT_FOUND`, `SKU_INACTIVE`,
`PRODUCT_NOT_SELLABLE`, `QUANTITY_OVERFLOW`, and `INSUFFICIENT_STOCK`.
Business rejection is represented in the result and does not abort valid items.

### R-C06-04 — Transactional sellability and mutation

- SKU existence, active state, product existence, product published state,
  draft state, cumulative quantity overflow, and current stock are checked on
  the backend.
- Valid items use the current SKU price. All valid mutations and the durable
  ledger commit atomically; an infrastructure/database exception rolls back
  both cart changes and the ledger row.
- An active cart is not created when every item is business-rejected.
- Existing add/update/remove/clear cart behavior remains unchanged.

### R-C06-05 — Single frontend orchestration

- `CartInitializer` is the only guest-cart merge orchestrator. The sign-in form
  performs authentication and navigation only.
- One `mergeId` is persisted per local merge attempt. Network retries reuse it;
  it is cleared only after every result has been applied locally.
- The frontend removes only `MERGED` local entries. `REJECTED` entries retain
  their original quantity in local storage and get a Vietnamese reason message.
- A failed merge is not reported as success and never clears local entries.

## Public API

```http
POST /cart/merge
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "mergeId": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    { "skuId": 17, "quantity": 2 },
    { "skuId": 42, "quantity": 1 }
  ]
}
```

```json
{
  "mergeId": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    {
      "skuId": 17,
      "requestedQuantity": 2,
      "mergedQuantity": 2,
      "status": "MERGED",
      "reasonCode": null
    },
    {
      "skuId": 42,
      "requestedQuantity": 1,
      "mergedQuantity": 0,
      "status": "REJECTED",
      "reasonCode": "INSUFFICIENT_STOCK"
    }
  ]
}
```

HTTP error mapping follows the existing `{code, message, result}` contract:
validation errors are `400`, unauthenticated is `401`, merge-id reuse is `409`,
and Redis preflight failure is `503`.

## Acceptance criteria

- `AC-C06-01`: One batch endpoint replaces N sequential add-to-cart calls.
- `AC-C06-02`: Replaying one merge ID does not add quantity twice.
- `AC-C06-03`: Redis cache miss/failure after commit cannot break durable
  idempotency or cause a second mutation.
- `AC-C06-04`: Partial rejection preserves valid server merges and rejected
  local entries.
- `AC-C06-05`: There is one frontend orchestration point.
- `AC-C06-06`: Backend enforces sellability and stock rules.
- `AC-C06-07`: Focused backend and frontend harness checks pass where runnable.
- `AC-C06-08`: No stage, commit, or push is performed by the implementation.

## Non-goals

Guest checkout, checkout-page UI, stock reservation, order creation, Flyway
finalization, and changing ordinary cart mutations are out of scope.
