# Commerce-09 — Admin order management

Status: **Accepted for implementation** (explicit user authorization, 2026-08-27)

This specification defines the admin-facing order list, detail, filtering, and
status-transition UI. Plan 05 remains a prerequisite for the backend lifecycle
contract; its automated checks pass but human UAT/sign-off is still pending.

## Requirements

### R-C09-01 — Backend-driven list and URL filters

The admin order page must load from `GET /admin/orders` and treat the URL as
the source of truth for `search`, `status`, `fromDate`, `toDate`, `page`, and
`size`. Search is trimmed, dates use the local UI date range serialized to
backend `Instant` boundaries, invalid status values are ignored, and
`fromDate > toDate` is rejected without an API call. Applying a filter resets
the page to zero. Only backend-approved page sizes are offered.

### R-C09-02 — Canonical order detail

`/admin/orders/[orderCode]` must load `GET /admin/orders/{orderCode}` and show
the order code, timestamps, recipient/address snapshot, item snapshots,
amounts, voucher, payment state, and immutable status timeline. A missing
order is rendered as a safe not-found state; backend error text is not exposed.

### R-C09-03 — Authoritative status actions

The detail view renders actions only for targets present in the backend
`allowedTransitions` response. Supported labels are PENDING (Chờ xác nhận),
CONFIRMED (Đã xác nhận), SHIPPING (Đang giao), DELIVERED (Đã giao), and
CANCELLED (Đã hủy). Cancellation requires a trimmed reason; all notes are
limited to the backend 500-character contract. Mutations submit through a
Server Action, disable duplicate submission, revalidate the list/detail, and
refresh authoritative data after a `409` conflict without automatic retry.

### R-C09-04 — Authorization and error boundaries

The admin route guard and backend `ADMIN` authorization both remain required.
Unauthenticated/non-admin callers must not receive admin order data. `403`,
`404`, `409`, validation, and unavailable-backend failures map to Vietnamese,
customer-safe guidance; raw backend messages are never rendered.

### R-C09-05 — Admin/customer boundary and responsive UX

Admin order components live under the admin feature boundary and do not import
customer order UI. Preserve the existing admin layout, SideMenu, voucher
management, and unrelated dirty worktree changes. The list is usable as a
desktop table and mobile cards, with typed loading, empty, filter-empty, and
error states.

## Acceptance criteria

- **AC-C09-01:** URL filters round-trip deterministically; search is trimmed,
  invalid status is ignored, date order is validated, and page resets on a new
  filter.
- **AC-C09-02:** Admin list renders backend data with code, date, recipient,
  status, payment, item preview/count, total, detail link, pagination, and all
  four loading/empty/error states.
- **AC-C09-03:** Detail renders snapshots, amounts, payment, voucher, items,
  and timeline from the backend response.
- **AC-C09-04:** Only `allowedTransitions` actions are visible; no invented
  `SHIPPED` or `REFUNDED` status is used.
- **AC-C09-05:** Cancellation blocks blank/overlong reasons and all submitted
  notes are trimmed and at most 500 characters.
- **AC-C09-06:** Successful transitions revalidate and refresh; a `409` shows
  conflict guidance and refreshes authoritative detail; duplicate submits are
  prevented.
- **AC-C09-07:** Admin route/backend authorization is preserved and raw API
  errors are hidden from users.
- **AC-C09-08:** Admin UI remains responsive and does not cross-import customer
  order components; SideMenu and existing admin modules remain intact.
- **AC-C09-09:** Focused deterministic harness, typecheck, scoped lint, and
  production build are run; unavailable browser/live-backend checks are marked
  `NOT VERIFIED` and human UAT remains `UAT PENDING`.

## API contract

```text
GET   /admin/orders?search=&status=&fromDate=&toDate=&page=0&size=20
GET   /admin/orders/{orderCode}
PATCH /admin/orders/{orderCode}/status
      { "status": "CONFIRMED", "note": "optional note" }
```

The backend response is authoritative for `allowedTransitions`, totals,
snapshots, and history. No database migration or backend lifecycle rewrite is
in scope for Commerce-09.

## Verification and handoff

The repository has no installed component/E2E runner, so the focused harness is
a Node deterministic contract harness. Browser, live Spring backend,
PostgreSQL/Redis, and human UAT are reported separately as `NOT VERIFIED` or
`UAT PENDING`. No stage, commit, or push is performed by this plan.

Traceability: R-C09-01 → AC-C09-01/02; R-C09-02 → AC-C09-03; R-C09-03 →
AC-C09-04/05/06; R-C09-04 → AC-C09-07; R-C09-05 → AC-C09-08/09.
