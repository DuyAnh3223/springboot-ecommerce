# Commerce-07 - Customer Checkout UI

## Status and authority

Accepted for implementation by the explicit implementation request for Plan
Commerce-07 on 2026-08-25. This specification fills the missing durable
product contract identified by the plan. It does not change the accepted
reviewed-snapshot decision in ADR-002 or add guest checkout.

## Current behavior and evidence

- The cart has selectable SKU rows and a checkout button, but the button does
  not navigate to a checkout flow.
- There is no customer checkout route or checkout feature under `client/`.
- The backend already exposes authenticated `POST /orders/checkout-review`,
  authenticated `POST /orders` with `Idempotency-Key`, and owner-safe
  `GET /orders/{orderCode}`.
- Existing address services expose the authenticated address list and address
  CRUD contracts.
- Baseline on 2026-08-25: `npm.cmd exec tsc -- --noEmit --pretty false`
  passed; `npm.cmd run build` passed with expected backend connection errors
  while prerendering unrelated data-backed pages; `npm.cmd run test:guest-cart`
  passed 4/4. Full lint is not a clean baseline because existing files report
  134 errors and 66 warnings outside this feature.
- The client currently has no React component or browser E2E runner for
  checkout.

## Goal

Authenticated customers can check out only the selected cart items, review
server-authoritative amounts, choose an existing or new address, place a COD
order safely under retries and changed-review conflicts, and reach an
owner-safe order success page. Guests are sent through sign-in and retain the
full checkout callback URL.

## Requirements

### R-C07-01 - Selected-item URL and authentication flow

- The cart checkout CTA navigates to `/checkout?skuIds=17,42` using selected
  SKU IDs sorted ascending and deduplicated.
- With no selected items, the CTA is disabled and does not navigate.
- Refreshing checkout preserves the URL selection.
- Checkout is authenticated-only. A guest is redirected to sign-in with the
  complete encoded checkout callback URL, including its query string.
- After sign-in, the existing guest-cart merge flow remains responsible for
  merging local items; successful authentication then returns to the callback.
- Invalid, duplicate, or non-positive IDs are normalized out. An empty result
  returns the customer safely to cart with a Vietnamese explanation or an
  equivalent safe empty state.

### R-C07-02 - Authoritative review rendering

- Checkout review calls `POST /orders/checkout-review` with selected SKU IDs
  and the normalized voucher code when present.
- The displayed items, quantities, prices, shipping, discount, voucher
  applicability, total, and placement eligibility come only from the latest
  server response.
- The client does not calculate or optimistically update checkout money.
- Review item and voucher issue codes are mapped to Vietnamese customer-facing
  messages. Raw backend English messages are not rendered as the primary UI.
- A response with `canPlaceOrder=false` disables order submission and shows the
  applicable issue(s) without inventing stock or amount values.

### R-C07-03 - Address and payment form

- The form uses React Hook Form and models exactly one address mode:
  `EXISTING` with `addressId`, or `NEW` with a validated new address payload.
- The create request maps the new-address payload to the backend
  `newUserAddress` field. `addressId` and `newUserAddress` are mutually
  exclusive and one is required.
- Payment is COD only and every valid create request sends
  `paymentMethod: "COD"`. No disabled online-payment placeholder is added.
- Existing addresses are loaded from the existing address service. New address
  validation reuses the address field rules without changing profile-address
  behavior.
- Voucher input is trimmed and normalized to uppercase. Apply/remove always
  triggers a fresh authoritative review using the same selected IDs.

### R-C07-04 - Idempotent submission UX

- A valid semantic create attempt obtains one UUID idempotency key using
  `crypto.randomUUID()` and sends it in `Idempotency-Key`.
- Double-clicks, loading retries, and network-timeout retries for the same
  normalized payload reuse the same key. The client does not generate a new
  key merely because a request timed out.
- The create payload includes the latest reviewed checkout expectation snapshot:
  item SKU/quantity/unit price/line total, voucher identity/applicability,
  eligible subtotal, subtotal, discount, shipping, total, the selected address,
  and COD payment method. Display-only fields and a fingerprint/token are not
  added.
- Submission uses the shared async-action pattern and prevents concurrent
  creates from the same form.
- A successful response navigates to `/checkout/success?orderCode=...` using
  the encoded returned order code.

### R-C07-05 - Changed-review and idempotency conflicts

- A `409 CHECKOUT_CHANGED` response replaces the displayed review with the
  latest review returned by the server, preserves the address form, and asks
  the customer to confirm the changed amounts/items before another submit.
- Confirming a changed review starts a new semantic attempt with a new
  idempotency key. The client never auto-submits the changed order.
- A `409 IDEMPOTENCY_KEY_REUSED` response is treated as a safe conflict: the
  client does not retry with a random key and a different payload. It refreshes
  review and asks the customer to retry intentionally.
- Other validation, authentication, unavailable-service, and business errors
  remain on the checkout page with Vietnamese guidance where recovery is
  possible.

### R-C07-06 - Owner-safe success page

- The success route is a Server Component that fetches
  `GET /orders/{orderCode}` using the current authenticated session.
- Order details shown on success come from that response, including order
  code, COD/payment status, total, and address summary.
- Missing or foreign orders render the existing not-found behavior and do not
  expose order existence. Success data is not trusted from navigation state.
- The page provides links to continue shopping and view the customer's orders.

### R-C07-07 - Vietnamese responsive customer boundary

- Checkout and success UI are Vietnamese, responsive from mobile through
  desktop, keyboard accessible, and use labels/live status for loading and
  errors.
- Checkout code belongs under `client/features/customer/checkout/`; shared
  order request/response types and backend-only service methods belong under
  `client/features/orders/`.
- Customer checkout does not import admin UI, add checkout data to the cart
  Zustand store, or implement order-history/admin-order behavior.

## Acceptance criteria

### AC-C07-01 - Partial selection callback

Given a cart with multiple rows and two selected SKU IDs, when the customer
clicks checkout, then the URL contains the sorted, deduplicated selected IDs;
when no row is selected, the button is disabled and the URL does not change.
Maps to R-C07-01.

### AC-C07-02 - Guest callback and refresh

Given a guest opens a checkout URL, when authentication is required, then
sign-in receives the complete encoded checkout callback and a successful login
returns to the same selected checkout; refreshing retains the query selection.
Maps to R-C07-01.

### AC-C07-03 - Server-authoritative review

Given selected SKU IDs, when checkout loads or the voucher changes, then the
UI renders the latest review response and does not derive a competing total
from client-side arithmetic. Maps to R-C07-02 and R-C07-03.

### AC-C07-04 - Address XOR and COD

Given the checkout form, when the customer submits, then exactly one existing
address or new address is sent, and payment method is always COD. Invalid
forms cannot call create-order. Maps to R-C07-03.

### AC-C07-05 - Invalid voucher remains recoverable

Given an invalid or inapplicable voucher, when apply fails, then the page stays
usable, no optimistic discount is shown, and the customer can remove/correct
the voucher before submission. Maps to R-C07-02 and R-C07-03.

### AC-C07-06 - Idempotent create and timeout retry

Given a valid review and address, when submit is double-clicked or a request
times out and is retried, then only one idempotency key is used for that
semantic attempt and the UI prevents concurrent creates. Maps to R-C07-04.

### AC-C07-07 - Changed checkout confirmation

Given the server returns `CHECKOUT_CHANGED`, when the latest review is shown,
then no order is auto-created; confirming the changed review generates a new
attempt key and submitting again uses the latest snapshot. Maps to R-C07-05.

### AC-C07-08 - Owner-safe success

Given a successful create response, when success loads, then it fetches the
order by returned code under the current session and shows server data; a
missing or foreign code renders not-found. Maps to R-C07-06.

### AC-C07-09 - Scope and accessible Vietnamese UI

Given customer checkout is rendered at mobile and desktop widths, then it is
responsive, keyboard usable, Vietnamese, and does not import admin components
or mutate the cart store with checkout state. Maps to R-C07-07.

### AC-C07-10 - Verification boundary

Focused checkout unit/component harnesses, TypeScript, changed-file lint, and
build checks pass where the environment permits. Browser interaction,
PostgreSQL/Redis concurrency, and human product acceptance are reported
separately as UAT PENDING when not verified. Maps to all requirements.

## Non-goals

- Guest checkout or unauthenticated order creation.
- Payment providers, online payment UI, or payment callbacks.
- Cart persistence redesign, cart voucher arithmetic, stock reservation, or
  order-history/admin-order features.
- Changing the backend reviewed-snapshot or idempotency contracts from
  ADR-002.
- Making a checkout session, price hold, stock hold, voucher hold, fingerprint,
  or signed review token.

## Edge cases

- Duplicate, malformed, non-positive, or unavailable selected SKU IDs.
- Cart selection changes in another tab between cart and review.
- Empty cart or all selected items becoming unsellable.
- Review with insufficient stock, inactive SKU, unavailable product, or an
  inapplicable/expired/per-user-limited voucher.
- Address deleted or no longer owned before create.
- Double-click, timeout, transient 503, browser refresh during submission, and
  replay of a used idempotency key.
- Foreign, malformed, expired, or missing order code on success.

## Domain invariants

- The server is authoritative for all order-affecting amounts and eligibility.
- The submitted expectation snapshot is untrusted comparison input only.
- No order is created from a stale snapshot without explicit customer review
  of the latest server response.
- A semantic retry uses the same idempotency key; a new semantic payload uses a
  new key.
- Exactly one address mode and COD payment are required for create order.
- Success details are authorized by the backend owner-safe order endpoint.

## API contract

### Review request

```json
{
  "selectedSkuIds": [17, 42],
  "voucherCode": "SUMMER"
}
```

`voucherCode` is omitted when no voucher is applied. The response is the
backend `CheckoutResponse` wrapped in the existing API result shape:

```json
{
  "items": [
    {
      "skuId": 17,
      "skuCode": "SKU-17",
      "productName": "Example",
      "imageUrl": null,
      "quantity": 2,
      "unitPrice": 100000,
      "lineTotal": 200000,
      "availableStock": 5,
      "issueCode": null
    }
  ],
  "subtotal": 200000,
  "eligibleSubtotal": 200000,
  "shippingFee": 30000,
  "discountAmount": 20000,
  "totalAmount": 210000,
  "voucher": { "code": "SUMMER", "applicable": true, "issueCode": null },
  "canPlaceOrder": true
}
```

### Create request

```json
{
  "reviewedCheckout": {
    "items": [
      { "skuId": 17, "quantity": 2, "unitPrice": 100000, "lineTotal": 200000 }
    ],
    "subtotal": 200000,
    "eligibleSubtotal": 200000,
    "shippingFee": 30000,
    "discountAmount": 20000,
    "totalAmount": 210000,
    "voucher": { "code": "SUMMER", "applicable": true },
    "canPlaceOrder": true
  },
  "addressId": "address-uuid",
  "newUserAddress": null,
  "paymentMethod": "COD"
}
```

The frontend sends `addressId: null` with `newUserAddress` for a new address,
or the selected `addressId` with `newUserAddress: null` for an existing one.
The request is sent to `POST /orders` with an `Idempotency-Key` UUID. The
success result is the backend `OrderResponse`, including `orderCode`,
`status`, `subtotalAmount`, `shippingFee`, `discountAmount`, and `totalAmount`.

### Errors

- `400`: validation, address XOR, COD, invalid selection, or review not
  placeable; show field/business guidance.
- `401`: send the customer to sign-in while preserving callback when the
  session is absent or expires.
- `404`: order not found/foreign on success; render not-found.
- `409 CHECKOUT_CHANGED`: consume the latest checkout result, replace review,
  and require confirmation.
- `409 IDEMPOTENCY_KEY_REUSED`: do not invent a new key for the same payload;
  re-review and ask for an intentional retry.
- `503`: keep the form data and allow a bounded retry with the same key when
  the payload is unchanged.

## Security and authorization

- Checkout review, create order, address retrieval, and success detail use the
  current authenticated session; no user ID is supplied by the customer UI.
- The callback URL is encoded and restricted to the local application checkout
  path; arbitrary external redirects are not accepted.
- Client monetary values never become persistence authority. Backend
  recomputation and owner-safe order detail remain mandatory.

## Data and persistence considerations

- No client checkout state is persisted in Zustand, localStorage, or a new
  checkout database table.
- The backend owns order, stock, voucher, address snapshot, and idempotency
  persistence. This feature only consumes those contracts.

## Verification strategy

### Unit

- Test selected-ID normalization, URL/callback construction, request mapping,
  review issue mapping, and idempotency-attempt state transitions.

### Component / harness

- Add a focused deterministic frontend harness for CTA selection, voucher
  review replacement, address XOR/COD mapping, double-submit, timeout retry,
  changed-review confirmation, and success navigation.
- If a full browser/component framework is unavailable, keep the harness at
  pure state/request boundaries and mark browser UAT not verified.

### Integration

- Reuse existing backend order/address integration coverage where available;
  do not claim PostgreSQL/Redis or browser acceptance from TypeScript/build
  alone.

### Static / build / runtime

- Run TypeScript, changed-file ESLint, the focused checkout harness, build,
  `git diff --check`, and GitNexus `detect_changes` before handoff.
- Report pre-existing full-lint failures separately from feature failures.

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
|---|---|---|---|
| AC-C07-01 | checkout state/request harness | Select partial cart and click checkout | PLANNED |
| AC-C07-02 | callback URL harness | Guest sign-in and refresh checkout | PLANNED |
| AC-C07-03 | review mapping harness | Compare UI against API response | PLANNED |
| AC-C07-04 | form/request harness | Existing/new address and COD submit | PLANNED |
| AC-C07-05 | voucher state harness | Invalid then remove/correct voucher | PLANNED |
| AC-C07-06 | idempotency state harness | Double-click and timeout retry | PLANNED |
| AC-C07-07 | changed-review harness | Change price then reconfirm | PLANNED |
| AC-C07-08 | success loader harness | Open own and foreign order code | PLANNED |
| AC-C07-09 | changed-file lint/build | Keyboard/mobile/desktop review | PLANNED |
| AC-C07-10 | verification command log | Human sign-off | PLANNED |

## Acceptance scenarios

1. An authenticated customer selects two of three cart rows, reaches checkout,
   reviews server amounts, chooses an address, and places one COD order.
2. A guest reaches checkout, signs in, completes the existing cart merge, and
   returns to the same selected checkout URL.
3. A voucher apply/remove or changed cart price refreshes the review; the UI
   never shows a client-calculated replacement total.
4. A timeout retry and double-click use one idempotency key; a changed review
   requires explicit reconfirmation before a new attempt.
5. The success page loads the returned order code under the current session;
   a foreign code renders not-found.

## Open questions

None for the v1 scope. Existing backend contracts and ADR-002 define the
remaining product decisions; any change to payment methods, reservations, or
checkout persistence requires a new requirement and ADR/spec update.
