# ADR-004 — Voucher Redemption as the Canonical Usage Ledger

## Status

Accepted

Accepted by: user, 2026-08-19.

## Context

Voucher usage is currently represented by `voucher.usedCount`, duplicate rows
in the `voucher_user` join table, the voucher reference on `Order`, and a
`VoucherRedemption` row. Keeping both `voucher_user` and
`VoucherRedemption` creates two detailed usage sources that can diverge.
`voucher_user` also has no order identity or redemption status, so it cannot
reverse exactly one order's usage safely.

Order cancellation before COD delivery must restore one voucher use exactly
once. Refunds and returns after delivery are separate post-payment policies and
must not implicitly reactivate the original campaign voucher.

## Decision

- Keep one `VoucherRedemption` per voucher-bearing order as the canonical
  detailed usage ledger.
- Remove the `voucher_user` relationship and table.
- Count `maxPerUser` from `VoucherRedemption` rows in status `REDEEMED`.
- Keep `voucher.usedCount` as the atomic aggregate used to guard `maxUses`.
  Update the aggregate and redemption ledger in the same transaction.
- Order creation inserts `REDEEMED`. Plan 05 cancellation conditionally changes
  `REDEEMED` to `REVERSED`; only the successful transition decrements
  `usedCount`.
- Delivery, return, and refund do not reverse the original redemption by
  default. Refunds use the net amount actually paid. A seller-fault goodwill
  policy may issue a separate replacement voucher later.
- Manual correction must be audited with actor and reason; it must not directly
  overwrite `usedCount` without a corresponding ledger action.

## Rationale

The redemption row provides the missing order identity and lifecycle needed for
exact-once cancellation while replacing, rather than supplementing, the old
join table. Keeping `usedCount` preserves a cheap atomic global-cap guard;
redemptions provide per-user counting and reconciliation evidence.

## Alternatives Considered

### Use only `Order.voucher`

This is the smallest schema, but couples voucher consumption to order status
and provides no independent reversal or manual-adjustment lifecycle.

### Keep `voucher_user` only

It can count usage per user but cannot identify or reverse one order's usage,
and duplicate pairs have no stable identity.

### Keep both detailed tables

This minimizes immediate code change but preserves duplicate sources and the
risk of inconsistent counts.

### Use a full append-only voucher event ledger now

It best supports complex adjustments, but is beyond the accepted create/cancel
scope. It may replace the status row if future requirements need multiple
auditable adjustments per redemption.

## Consequences

### Positive

- One canonical per-order/per-user voucher usage record.
- Exact-once cancellation reversal through a conditional status transition.
- `maxPerUser` counts only active redemptions.
- Historical reversed rows remain available for audit and abuse analysis.

### Negative

- `usedCount` is a denormalized aggregate and must be reconciled with active
  redemptions.
- Refund, return, replacement-voucher, and manual-adjustment workflows still
  require accepted product policies before implementation.

## Constraints for Future Changes

- Do not recreate a second user-voucher usage table beside
  `VoucherRedemption`.
- Enforce unique `order_id` and index active per-user usage queries.
- Changes to redemption status and `usedCount` must share a transaction.
- Partial refunds require per-item net-paid/discount allocation; they must not
  infer refund amounts from the redemption row alone.
- Post-delivery restoration of a campaign voucher requires an explicit policy
  or a replacement-voucher flow, not an automatic order-status mapping.

## Supersedes

The `voucher_user` usage model retained by Commerce 02 and supplemented by the
initial Plan 04 implementation; no earlier ADR is superseded.

## Superseded By

None.
