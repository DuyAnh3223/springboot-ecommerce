# SPEC-COMMERCE-08 — Customer Order History, Detail, and Cancellation UI

## Status

Accepted for implementation by the explicit user request on 2026-08-27.
Plan 05 backend UAT remains pending; this specification relies only on its
implemented, automated owner-safe API contract and does not claim human
acceptance of the backend workflow.

## Context

The current `/profile/orders` page calls the removed arbitrary-user endpoint,
uses `any[]`, maps the nonexistent `SHIPPED` status, and renders only a minimal
summary. The backend now exposes authenticated owner-safe list, detail, and
cancel endpoints with typed snapshots and `allowedTransitions`.

## Current Behavior and Evidence

- Current route: `client/app/(customers)/profile/orders/page.tsx`.
- Existing API boundary: `client/features/orders/services/order.service.ts`.
- Backend contract: `GET /orders/me`, `GET /orders/{orderCode}`, and
  `POST /orders/{orderCode}/cancel`.
- Baseline `npm.cmd exec tsc -- --noEmit --pretty false`: PASS on 2026-08-27.
- Baseline scoped ESLint: pre-existing FAIL from `any[]` and three unused
  imports in the current order page.
- Existing checkout and guest-cart harnesses: PASS, 5/5 and 4/4.

## Goal

Authenticated customers can browse their own paginated order history, filter
it through URL state, inspect authoritative order snapshots, and cancel only
orders the backend currently allows, with safe conflict handling and
Vietnamese responsive UI.

## Requirements

### R-C08-01 — Typed owner-safe order contract

- The client uses exactly five fulfillment statuses: `PENDING`, `CONFIRMED`,
  `SHIPPING`, `DELIVERED`, and `CANCELLED`.
- One typed metadata mapping provides the Vietnamese label, badge tone, and
  progress position for every status. `SHIPPED` and `REFUNDED` are not used.
- The list service calls `GET /orders/me`; it never accepts or sends a user ID.
- List, detail, item, history, page, query, cancel request, and action results
  are typed. Backend English messages are not rendered as primary UI copy.

### R-C08-02 — URL-driven order list

- `/profile/orders` is a Server Component and reads `status`, `page`, and
  `size` from `searchParams`.
- The default page is UI page 1 backed by API page 0; the default size is 10.
  Invalid pages fall back to 1, and supported page sizes are 10, 20, and 50.
- Status filter and pagination produce canonical URLs so refresh and browser
  back/forward preserve the current view.
- Every row/card shows order code, Vietnamese creation date, status, payment
  method/status, item count/preview, total amount, and a detail link.
- Empty and recoverable service-error states are explicit, Vietnamese, and do
  not expose raw backend errors.

### R-C08-03 — Owner-safe snapshot detail

- `/profile/orders/[orderCode]` is a Server Component that calls the existing
  owner-safe `GET /orders/{orderCode}` service.
- A missing or foreign order uses the safe not-found UI; the client does not
  distinguish ownership from absence.
- The page renders persisted item, address, voucher, money, payment, and status
  snapshots from the order response. It does not refetch live product or
  address data to overwrite order history.
- Status history is rendered in backend response order, with a stable fallback
  for legacy `status` when `toStatus` is absent.
- `eligibleSubtotal` is not invented because the current detail DTO does not
  expose it.

### R-C08-04 — Backend-authoritative cancellation

- The cancel control is rendered only when `allowedTransitions` contains
  `CANCELLED`.
- The dialog uses React Hook Form and Zod. Reason is trimmed, required, and at
  most 500 characters, matching the backend DTO.
- Cancellation uses a Server Action and the shared `useAsyncAction`; concurrent
  submits are disabled.
- On success, the action revalidates `/profile/orders` and the matching detail
  route, then the client refreshes authoritative server data.
- `409 ORDER_STATUS_CONFLICT` is shown as a Vietnamese stale-state message and
  triggers refresh. The client never performs optimistic stock, voucher, or
  order-state compensation.
- Owner-safe `404`, validation `400`, authentication `401`, and unavailable
  service errors remain recoverable without exposing raw English messages.

### R-C08-05 — Vietnamese, responsive, accessible boundary

- Customer order UI lives under `client/features/customer/orders/`; shared
  order contracts/services/actions live under `client/features/orders/`.
- GET data is read by route Server Components through services; only cancel is
  a Server Action. No order entity list is cached in Zustand.
- New copy is Vietnamese. List and detail remain usable from mobile to desktop.
- Filters, links, dialog fields, errors, loading state, and close/cancel actions
  have labels and keyboard-usable controls.

## Acceptance Criteria

### AC-C08-01 — Typed canonical statuses

Given customer order source is checked, when typecheck and the order harness
run, then there is no `any[]`, unused order-page import, `SHIPPED`, or
`REFUNDED`, and all five backend statuses map to Vietnamese metadata.
Maps to R-C08-01.

### AC-C08-02 — Owner-safe list API

Given an authenticated customer opens order history, when the list is fetched,
then the client calls `/orders/me` with typed page/status parameters and never
builds an order URL from a user ID. Maps to R-C08-01 and R-C08-02.

### AC-C08-03 — URL state round-trip

Given a status, page, and supported size, when the customer filters or changes
page, then the canonical URL preserves those values and reloads the same view;
invalid values fall back safely. Maps to R-C08-02.

### AC-C08-04 — Snapshot owner-safe detail

Given an owned order code, when detail loads, then persisted order snapshots
and history are rendered; a missing or foreign code produces the same safe 404
UI. Maps to R-C08-03.

### AC-C08-05 — Allowed cancellation with validated reason

Given detail `allowedTransitions` contains `CANCELLED`, when the customer enters
a valid reason and confirms, then one cancel action is submitted and list/detail
data are revalidated. Without that transition or with an invalid reason, no
cancel request can be made. Maps to R-C08-04.

### AC-C08-06 — Conflict refresh

Given the displayed order becomes stale and cancel returns `409`, when the
action completes, then Vietnamese conflict guidance is shown and authoritative
server data is refreshed without optimistic compensation. Maps to R-C08-04.

### AC-C08-07 — Responsive accessible Vietnamese UI

Given mobile and desktop layouts, when the customer navigates list/detail and
the cancel dialog by keyboard, then content remains readable, controls remain
labeled, and raw backend English is not primary UI copy. Maps to R-C08-05.

### AC-C08-08 — Verification and repository boundary

Given implementation is handed off, then focused order tests, surrounding
checkout/cart tests, changed-file lint, typecheck, build, diff check, and
GitNexus change detection are reported accurately; browser/live-backend UAT is
`UAT PENDING` until a human signs off. No stage, commit, or push is performed.
Maps to all requirements.

## Non-Goals

- Admin order UI or backend lifecycle changes.
- Refund, return, reorder, product review, or shipping-provider tracking.
- Optimistic stock/voucher compensation or order caching in Zustand.
- A new persistence model or changes to order snapshots.
- Global lint cleanup.

## Edge Cases

- Missing, array-valued, unknown, or malformed URL parameters.
- Empty result page after a filter/page change.
- Missing preview image or legacy history entry with only `status`.
- Missing/foreign/malformed order code.
- Blank or over-500-character cancel reason.
- Double submit, expired authentication, backend unavailable, and stale
  `allowedTransitions` returning `409`.
- Already-cancelled orders returned idempotently by the backend.

## Domain Invariants

- The backend is authoritative for ownership, status, transitions, history,
  amounts, stock, and voucher state.
- `allowedTransitions`, not the displayed status alone, governs cancel
  visibility.
- Persisted order item/address/money snapshots are history; live catalog data
  must not overwrite them.
- Owner failure is always a nondisclosing 404 boundary.

## API Contract

### List request

```text
GET /orders/me?page=0&size=10&status=PENDING
```

`status` is omitted for the all-orders view. Response result is a Spring page
of typed order summaries, including `previewItem` and `allowedTransitions`.

### Detail request

```text
GET /orders/{encodedOrderCode}
```

The result contains the order, payment and address snapshots, all item
snapshots, ordered history, and customer `allowedTransitions`.

### Cancel request

```text
POST /orders/{encodedOrderCode}/cancel
{ "reason": "Tôi muốn thay đổi sản phẩm" }
```

Reason is trimmed, required, and limited to 500 characters.

### Errors

- `400`: invalid query/reason; show Vietnamese validation guidance.
- `401`: redirect or guide the customer to sign in with a local callback.
- `404` / code `1034`: safe order-not-found UI.
- `409` / code `1069`: status conflict; refresh authoritative order state.
- `503`: keep the dialog data and allow retry after guidance.

## Security / Authorization

- List, detail, and cancel use the current authenticated session token.
- The frontend never sends an arbitrary customer user ID.
- Detail and cancel preserve the backend nondisclosing owner-safe 404 behavior.
- Server Actions do not trust route rendering as authorization; the backend
  validates the authenticated owner on every mutation.

## Data / Persistence Considerations

No new client or backend persistence is introduced. Orders remain backend-owned
and are refetched after cancellation.

## Verification Strategy

### Unit / deterministic behavior harness

- Status metadata, query parsing/serialization, date/payment labels.
- Cancel visibility, reason normalization/validation, and conflict mapping.
- Snapshot/history fallbacks and raw-error isolation.

### Component / browser

- Static/type/build checks exercise Server/Client boundaries.
- Browser interaction and dialog focus/responsive behavior require manual UAT
  because the repository has no installed component/E2E runner.

### Integration

- Reuse Plan 05 backend controller/service/integration evidence for the API
  contract; live backend list/detail/cancel remains manual UAT for this feature.

### Static / Build / Runtime

- `npm.cmd run test:orders`
- `npm.cmd run test:checkout`
- `npm.cmd run test:guest-cart`
- changed-file ESLint
- `npm.cmd exec tsc -- --noEmit --pretty false`
- `npm.cmd run build`
- `git diff --check`
- `node .gitnexus/run.cjs detect-changes`

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
|---|---|---|---|
| AC-C08-01 | order harness + lint/typecheck | Status labels | PLANNED |
| AC-C08-02 | service/query harness | Observe `/orders/me` | PLANNED |
| AC-C08-03 | query harness | Filter/page back-forward | PLANNED |
| AC-C08-04 | detail types/build | Own/foreign detail | PLANNED |
| AC-C08-05 | cancel harness/build | Valid/invalid cancel | PLANNED |
| AC-C08-06 | conflict harness | Simulated stale cancel | PLANNED |
| AC-C08-07 | lint/build | Keyboard/mobile/desktop | PLANNED |
| AC-C08-08 | final command log | Human sign-off | PLANNED |

## Acceptance Scenarios

1. Browse all orders, filter by status, paginate, refresh, and use browser
   back/forward without losing URL state.
2. Open an owned order and confirm item, address, money, payment, and history
   snapshots; a foreign/missing code renders the same 404.
3. Cancel an allowed pending order with a valid reason and observe refreshed
   list/detail state.
4. Attempt blank/long/stale cancellation and receive Vietnamese guidance
   without double submission or optimistic compensation.
5. Verify no cancel control for disallowed states and audit keyboard/mobile UI.

## Open Questions

None for v1. The current backend DTO intentionally does not expose
`eligibleSubtotal`, so the UI must not fabricate it.
