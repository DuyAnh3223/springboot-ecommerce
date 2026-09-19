# Shipment regression repair and accepted UAT

## Outcome

The shipment module passed its regression checks and the isolated runtime UAT on 2026-09-19. UAT is accepted under the user's prior conditional authorization because every required scenario completed successfully.

## Repairs

- GHN quoting now runs before Redis locks and the database transaction. Order creation reloads the saved address in the transaction, rejects a changed or expired quote, and uses the verified fee for authoritative checkout recomputation.
- Checkout clears the old review and idempotency attempt before requesting a replacement quote. A stale response cannot restore a review after the customer changes address mode.
- GHN location failure code 1081 displays valid Vietnamese guidance.
- A successful admin status mutation performs a full page reload so status, allowed actions, history, and payment state are read back consistently from the backend.

## Automated verification

- Backend focused unit and integration verification passed, including 24 PostgreSQL `OrderIT` cases and the saved-address concurrency regression.
- Frontend checkout, admin-order, and customer-order suites passed: 21/21 tests.
- Affected frontend lint, TypeScript checking, and the Next.js production build passed.

## Runtime UAT

Environment: disposable PostgreSQL, real Redis, real GHN staging, isolated Spring Boot/Next.js processes, and Microsoft Edge. Temporary services and data were removed by the harness after completion.

All scenarios passed:

1. GHN returned province/district/ward lists and quotes for Hồ Chí Minh and Hà Nội routes; observed fees were 20,900, 20,900, and 64,900 VND.
2. An address change was rejected with `CHECKOUT_CHANGED` even when the fee stayed equal; stale fee/address attempts created no order, payment, or stock mutation.
3. Browser checkout produced a new usable quote after changing a saved address; a delayed saved-address response could not overwrite the new-address state.
4. With GHN disabled, the old quote was removed, order submission stayed blocked, API creation returned HTTP 503/code 1081, and no business mutation occurred.
5. After recovery, browser COD checkout created order `ORD-20260919-9B3E2D82` with subtotal 100,000 VND, fee 20,900 VND, total/payment 120,900 VND, and one stock decrement.
6. Admin completed `PENDING -> CONFIRMED -> SHIPPING -> DELIVERED`; COD became paid, four history entries existed, and delivery caused no second stock decrement.
7. An idempotent replay returned the same order without another stock mutation.
8. Delivery failure required an admin and a non-empty reason. The customer saw the failure, payment remained unpaid, stock was not restored or decremented again, and the terminal state rejected another transition.

Evidence is stored in `server/target/shipment-uat-results.json` and the accompanying `shipment-uat-*.png` screenshots.
