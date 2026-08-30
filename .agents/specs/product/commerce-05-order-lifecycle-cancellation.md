# SPEC-COMMERCE-05 — Order Lifecycle, Cancellation, and Compensation

## Status

Accepted by explicit user confirmation on 2026-08-30. Automated verification
and human UAT remain tracked separately from specification acceptance.

## Context

Plan 01–04 đã khóa create order COD: order `PENDING`/payment `UNPAID` được tạo
atomically kèm stock decrement (`SALE_OUT`) và voucher redemption
(`REDEEMED`), với `OrderItem` là nguồn committed quantity (ADR-003) và
`VoucherRedemption` là canonical usage ledger (ADR-004). Plan 04 đã chặn user
cancellation/status transitions và reversal (`REVERSED`) — đây chính là phạm vi
Plan 05. Chưa tồn tại admin order API, customer order history API, transition
policy dùng chung, hoặc compensation khi hủy.

`OrderStatus` hiện tại chứa `REFUNDED` (ngoài v1 fulfillment) và không có
`SHIPPING`; `PaymentStatus` chỉ có `UNPAID`. `OrderController` có
`GET /orders/user/{userId}` (IDOR) cần xóa. `OrderStatusHistory` lưu
`fromStatus`/`toStatus` chưa có (chỉ `status`), `createdBy` trỏ `User` —
cần actor type/identifier an toàn cho admin. Không có pessimistic lock query
cho mutation theo `orderCode`; `@Version` đã có từ Plan 04.

## Current Behavior and Evidence

- Relevant flow: `OrderController.createOrder` →
  `OrderService.createOrder` (lock → transaction) → `Order` `PENDING` +
  `PaymentStatus.UNPAID`; `InventoryService.reserveStock` ghi `SALE_OUT`;
  `OrderService.redeemVoucher` tạo `VoucherRedemption.REDEEMED` và tăng
  `usedCount` qua `VoucherRepository.increaseUsedCount`.
- `OrderController` endpoints: `POST /orders/checkout-review`, `POST /orders`
  (idempotent), `GET /orders/user/{userId}`.
- Existing contract/spec: SPEC-COMMERCE-02 (accepted), SPEC-COMMERCE-03
  (accepted), SPEC-COMMERCE-04 (accepted),
  ADR-002/003/004 (accepted), PLAN-COMMERCE-05.
- Baseline command: `./mvnw -Dtest="OrderServiceTest,*OrderController*,*Inventory*" test`
  từ `server/`.
- Baseline result: to be recorded in the implementation session (PASS expected).

## Goal

- Hoàn thiện order state machine v1 đúng bảng đã khóa, customer ownership APIs,
  admin management APIs và cancel compensation chính xác một lần trong điều
  kiện concurrent requests.

## Requirements

### R-C05-01: Fulfillment and Payment Transition Policy

- `OrderStatus` v1 gồm đúng năm giá trị: `PENDING`, `CONFIRMED`, `SHIPPING`,
  `DELIVERED`, `CANCELLED`. Không còn `REFUNDED`; không dùng `SHIPPED`.
- Transition policy dùng chung cho mọi actor, được mã hóa tại service/policy
  layer (không duplicate trong controller):

  | Actor | Current status | Allowed target |
  |---|---|---|
  | Customer owner | PENDING | CANCELLED |
  | Admin | PENDING | CONFIRMED hoặc CANCELLED |
  | Admin | CONFIRMED | SHIPPING hoặc CANCELLED |
  | Admin | SHIPPING | DELIVERED |
  | Bất kỳ | DELIVERED/CANCELLED | Không có transition mới |

- Payment mapping COD:
  - Tạo order: `UNPAID`.
  - Sang `DELIVERED`: `PAID`.
  - Hủy trước giao: `CANCELLED`.
- `DELIVERED` và `CANCELLED` là terminal. Không triển khai refund/return sau
  delivered (roadmap khác).
- `PaymentStatus` bổ sung `PAID`, `CANCELLED`.

### R-C05-02: Owner-Safe Customer APIs

- `GET /orders/me?page=0&size=10&status=PENDING` — current user qua
  `AuthService`; không nhận userId từ path/query. Status filter optional;
  sort mặc định newest first. Pagination có max size.
- `GET /orders/{orderCode}` — order không thuộc current user trả `404`
  `ORDER_NOT_FOUND`, không `403`, để không tiết lộ existence (IDOR-safe).
- `POST /orders/{orderCode}/cancel` — chỉ owner và current status `PENDING`.
  Body `{ "reason": "..." }` bắt buộc, trim, length hợp lý; lưu snapshot vào
  history.
- Xóa `GET /orders/user/{userId}` — không alias/redirect.
- Không gọi `SecurityContextHolder` trực tiếp trong controller/service
  production (server/AGENTS.md); dùng `AuthService`.

### R-C05-03: Authorized Admin APIs

- `GET /admin/orders?search=...&status=...&fromDate=...&toDate=...&page=0&size=20`
  — dùng `@PreAuthorize("hasRole('ADMIN')")` ở method boundary theo convention
  repo (controller `VoucherController`, service `UserService`).
- `search` tìm order code, recipient name hoặc phone theo query/specification
  an toàn (null-safe, parameterized, không nối SQL string).
- `fromDate/toDate` có instant/timezone semantics nhất quán; reject range đảo.
- Pagination có max size.
- `GET /admin/orders/{orderCode}` — admin detail đầy đủ.
- `PATCH /admin/orders/{orderCode}/status` — body
  `{ "status": "CONFIRMED", "note": "..." }`; admin transition gọi cùng service
  state-machine, không duplicate logic trong controller.
- `allowedTransitions` cho admin được tính từ cùng transition policy.

### R-C05-04: Exact-Once Cancellation Compensation

Tất cả bước trong một transaction, sau khi lock order row (pessimistic write
lock):

1. Validate actor/owner/current state.
2. Nếu order đã `CANCELLED`, trả order hiện tại (idempotent); không
   compensation lại.
3. Với từng `OrderItem` của order: atomic increment SKU stock theo persisted
   quantity (`increaseStock` với guard không âm), ghi `StockMovement` reason
   `ORDER_CANCEL_RETURN` tham chiếu order. Không đọc quantity từ cart/catalog;
   không có allocation state riêng. Locked order-status transition là
   exact-once guard.
4. Với canonical voucher redemption:
   - Conditional update `REDEEMED -> REVERSED` cho redemption của order
     (`reverseRedemptionByOrderId`), chỉ khi update count = 1 mới atomic decrement
     `usedCount` với guard không âm (`decreaseUsedCount`).
   - Nếu order không có voucher thì bỏ qua redemption.
   - Nếu order có voucher, phải có đúng một redemption `REDEEMED`; thiếu,
     đã reversed, hoặc update không đúng một row là integrity failure và phải
     rollback toàn transaction.
   - `decreaseUsedCount` cũng phải update đúng một row; kết quả khác một là
     failure, không được commit redemption reversal riêng lẻ.
5. Update order status `CANCELLED`, payment status `CANCELLED`, ghi history
   (fromStatus, toStatus, reason, actor type/identifier an toàn, timestamp).
6. Bất kỳ failure nào rollback toàn bộ compensation và status.

### R-C05-05: Deterministic History and Concurrency Behavior

- Repository cung cấp query load order by code với pessimistic write lock cho
  mutation.
- `Order` giữ `@Version` để phát hiện stale writes ngoài locked flow.
- Mỗi transition hợp lệ tạo đúng một `OrderStatusHistory` gồm `fromStatus`,
  `toStatus`, `note/reason`, actor type/identifier an toàn và timestamp.
- Read history sort deterministic theo timestamp rồi ID.
- Request target bằng current status trả current order idempotently, không tạo
  history mới.
- Từ terminal state sang status khác trả `409 ORDER_STATUS_CONFLICT`, không 400
  generic.

## Acceptance Criteria

### AC-C05-01: State Machine và Payment Transitions Đúng Bảng Đã Khóa

Given một order ở mỗi current status và một actor (customer owner / admin),
When một transition được yêu cầu,
Then transition hợp lệ theo bảng R-C05-01 được áp dụng, payment mapping COD
(`DELIVERED` → `PAID`, cancel trước giao → `CANCELLED`) được cập nhật cùng
transaction, và mọi transition không hợp lệ hoặc từ terminal state bị reject
với `409 ORDER_STATUS_CONFLICT` không mutation.

### AC-C05-02: Customer APIs Owner-Safe; IDOR Endpoint Đã Xóa

Given một authenticated customer,
When gọi `GET /orders/me`, `GET /orders/{orderCode}`, `POST /orders/{orderCode}/cancel`,
Then chỉ dữ liệu thuộc chính user được trả; order của user khác trả `404`
không lộ existence; `GET /orders/user/{userId}` không còn accessible.

### AC-C05-03: Admin APIs Được Bảo Vệ và Dùng Shared Service Policy

Given một authenticated non-admin và một admin,
When gọi `/admin/orders*`,
Then non-admin nhận 403; admin nhận dữ liệu/tra cứu theo filter, và mọi
transition gọi cùng service state-machine với `allowedTransitions` tính từ
cùng transition policy.

### AC-C05-04: History Đầy Đủ, Deterministic, Không Duplicate

Given các transition xảy ra,
When order được đọc,
Then mỗi transition hợp lệ có đúng một history entry (fromStatus, toStatus,
actor/note, timestamp); same-target request không tạo history mới; history
được sort theo timestamp rồi ID.

### AC-C05-05: Cancel Hoàn Stock/Voucher Đúng Một Lần Khi Concurrent/Retry

Given hai request cancel đồng thời hoặc retry cancel sau khi đã `CANCELLED`,
When chúng được xử lý,
Then chỉ một compensation xảy ra: stock tăng đúng tổng `OrderItem.quantity`
một lần với một `ORDER_CANCEL_RETURN` movement mỗi SKU, voucher `REDEEMED` →
`REVERSED` và `usedCount` giảm đúng một lần, và chỉ một history entry.

### AC-C05-06: Invalid Transition Trả 409

Given một transition không có trong bảng policy hoặc từ terminal state,
When transition được yêu cầu,
Then trả `409 ORDER_STATUS_CONFLICT` (hoặc error code tương đương), không 400
generic, và không mutation.

### AC-C05-07: PostgreSQL Tests Chứng Minh Transaction Rollback/Exact-Once

Given integration test trên PostgreSQL thật,
When cancel thành công, retry, concurrent cancel, và failure giữa các bước
compensation xảy ra,
Then stock/voucher/order/history rollback cùng transaction; concurrent/retry
chỉ một compensation.

### AC-C05-08: Không Stage/Commit/Push

Given toàn bộ work hoàn tất,
Then working tree không bị stage/commit/push; dừng sau handoff cho human review.

## Non-Goals

- Customer/admin frontend (Plan 08/09).
- Return/refund/online payment.
- Shipping provider integration.
- Flyway final baseline (Plan 10).
- Partial fulfillment, per-line cancellation, warehouse allocation, backorder.

## Edge Cases

- Reason thiếu/blank hoặc length > giới hạn → 400.
- Cancel order của user khác → 404 (không 403).
- Cancel order không tồn tại → 404.
- Same-target transition (target = current status) → trả order hiện tại, không
  history mới.
- Transition từ `DELIVERED`/`CANCELLED` → 409, không mutation.
- Concurrent cancel: request thua sau lock thấy `CANCELLED` → trả order hiện
  tại, không compensation.
- Retry cancel sau khi đã `CANCELLED` → trả order hiện tại, không compensation
  lần hai.
- Order không có voucher → không cần redemption, không decrement.
- Order có voucher nhưng redemption thiếu/đã reversed → integrity failure,
  rollback, không đổi trạng thái order.
- Failure giữa stock/voucher/status → rollback toàn transaction.
- `fromDate > toDate` → 400.
- Search/status/date không có giá trị → bỏ qua filter tương ứng.
- Status không hợp lệ → 400.
- `fromDate`/`toDate` lọc trên `createdAt`, đều inclusive; giá trị là ISO
  date-time có offset và được normalize về `Instant`.
- Với admin search, `page < 0` hoặc `size < 1` → 400; `size > 50` → clamp về
  50. Customer list giữ convention riêng của endpoint.

## Domain Invariants

- Database là nguồn sự thật duy nhất; compensation đọc quantity từ
  `OrderItem` đã persisted, không từ cart/catalog.
- Locked order-status transition là exact-once guard cho compensation.
- Stock restoration và status transition commit trong một transaction
  (ADR-003).
- `VoucherRedemption` là canonical usage ledger; `usedCount` là atomic
  aggregate, cập nhật cùng transaction với ledger (ADR-004).
- Không có allocation state riêng, không có `voucher_user` table.
- `DELIVERED`/`CANCELLED` terminal; không refund trong v1.

## API Contract

### Customer

```text
GET  /orders/me?page=0&size=10&status=PENDING
GET  /orders/{orderCode}
POST /orders/{orderCode}/cancel
```

Cancel body:

```json
{ "reason": "Tôi muốn thay đổi sản phẩm" }
```

- Reason bắt buộc, trim, max 500 chars; lưu vào history note.
- Order không thuộc current user trả `404 ORDER_NOT_FOUND`.

### Admin

```text
GET   /admin/orders?search=...&status=...&fromDate=...&toDate=...&page=0&size=20
GET   /admin/orders/{orderCode}
PATCH /admin/orders/{orderCode}/status
```

Status body:

```json
{
  "status": "CONFIRMED",
  "note": "Đã xác nhận qua hệ thống"
}
```

- `@PreAuthorize("hasRole('ADMIN')")` ở method boundary.
- `search` tìm order code, recipient name hoặc phone, parameterized.
- `fromDate/toDate` theo instant; reject range đảo (400).
- Pagination max size.

### Response contracts

- Order summary: order code, created/updated date, fulfillment status, payment
  method/status, subtotal/shipping/discount/total, item count và tối thiểu một
  item preview, `allowedTransitions` theo actor.
- Order detail: summary + address snapshot, toàn bộ item snapshots (SKU,
  product name, attributes/image nếu snapshot có, quantity, unit price, line
  total), voucher code/eligible subtotal/discount, status history tăng dần
  theo thời gian, actor/note/time không expose internal user secrets.
- Không trả entity JPA trực tiếp; dùng DTO response.

### Errors

- `400` — validation DTO, reason thiếu/quá dài, range date đảo.
- `401` — unauthenticated.
- `403` — non-admin gọi admin API.
- `404` — order không tồn tại hoặc không thuộc current user (không phân biệt).
- `409` — `ORDER_STATUS_CONFLICT` cho transition không hợp lệ/terminal.
- `500` — compensation/integrity failure; transaction rollback, không trả
  partial order state.

## Security / Authorization

- Customer resolve qua `AuthService` (`getCurrentUsername`); không nhận
  userId từ path/query.
- Customer detail/cancel owner-safe, `404` thay vì `403` khi không thuộc.
- Admin API dùng `@PreAuthorize("hasRole('ADMIN')")` (convention repo).
- History lưu actor type/identifier an toàn; response không expose internal
  user secrets (không trả toàn bộ entity `User`).

## Data / Persistence Considerations

- `OrderStatusHistory`: thêm `fromStatus`, `toStatus`, `actorType`,
  `actorId` (safe identifier, string), giữ `note`, `createdAt`; sort
  deterministic theo `createdAt` rồi `id`. `fromStatus/toStatus` là canonical
  cho transition mới; legacy `status` chỉ giữ để tương thích dữ liệu cũ và
  không được dùng thay thế `toStatus`. History tạo order có
  `fromStatus = null`, `toStatus = PENDING`, actor `CUSTOMER`; `createdBy` có
  thể giữ nullable cho legacy nhưng không được serialize ra response.
- `OrderRepository`: thêm query load by code với pessimistic write lock
  (`@Lock(PESSIMISTIC_WRITE)` + `@Query`), owner-aware reads, pageable
  customer list, admin search specification.
- `ProductSkuRepository`: thêm `increaseStock` atomic guard không âm.
- Voucher usage repository: thêm conditional `REDEEMED -> REVERSED`
  (`reverseRedemptionByOrderId`) và atomic `decreaseUsedCount` guard không âm;
  cả hai row-count phải được kiểm tra trong cùng transaction.
- `StockMovement.reason` dùng `ORDER_CANCEL_RETURN` (length 30 OK).
- `PaymentStatus`: thêm `PAID`, `CANCELLED`.
- Trước Plan 10: JPA update phục vụ development/test; ghi rõ schema
  incompatibility nếu có (không tạo legacy backfill vì production sẽ reset).

## Verification Strategy

### Unit

- Transition matrix đầy đủ cho customer/admin.
- Same-target idempotent không ghi history.
- Invalid transition trả 409 code.
- Customer owner check.
- Cancel reason validation.
- Allowed transitions theo actor/status.
- Delivered cập nhật payment `PAID`.

### MVC / Component

- `/orders/me` không nhận arbitrary user ID.
- User A lấy order user B nhận 404.
- Customer không gọi `/admin/orders`.
- Admin filter/date/page binding.
- Cancel/status response và validation contract.
- Legacy `/orders/user/{userId}` không còn accessible.

### Integration

- Cancel PENDING hoàn stock từ `OrderItem`, reverse voucher và ghi một history.
- Admin cancel CONFIRMED tương tự.
- Cancel SHIPPING/DELIVERED bị reject và không mutate.
- Hai concurrent customer/admin cancel chỉ một compensation.
- Repeated cancel không tăng stock/decrement voucher lần hai.
- Failure giữa stock/voucher/status rollback toàn transaction.
- Concurrent state updates không skip transition.

### Static / Build / Runtime

```bash
cd server
./mvnw -Dtest="OrderServiceTest,*OrderController*,*AdminOrder*,OrderIT,*Inventory*" test
./mvnw test
```

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
|---|---|---|---|
| AC-C05-01 | Transition matrix unit tests + delivered IT | UAT 2 | PLANNED |
| AC-C05-02 | MVC owner-safe tests + IDOR removed | UAT 1 | PLANNED |
| AC-C05-03 | MVC admin security/filter tests | UAT 2 | PLANNED |
| AC-C05-04 | History/idempotency unit tests | UAT 3 | PLANNED |
| AC-C05-05 | PostgreSQL cancel/race/retry IT | UAT 3 | PLANNED |
| AC-C05-06 | Invalid/terminal transition tests | UAT 4 | PLANNED |
| AC-C05-07 | PostgreSQL OrderIT with failure injection between compensation steps, plus exact-once/retry/race checks | UAT 3/4 | PLANNED |
| AC-C05-08 | `git status -s` | — | PLANNED |

## Acceptance Scenarios

1. As a customer, list/detail/cancel an owned `PENDING` order; non-owned code
   trả 404 không disclosure.
2. As an admin, exercise the allowed lifecycle `PENDING → CONFIRMED → SHIPPING
   → DELIVERED` và xác nhận COD payment thành `PAID` chỉ tại delivery; cancel
   `PENDING`/`CONFIRMED` hoàn stock/voucher.
3. Retry và concurrent cancel: một history entry, một stock/voucher
   compensation.
4. Invalid và terminal transitions: 409, không mutation.

## Open Questions

- Không còn material question: user đã chấp nhận authority,
  voucher-integrity failure behavior, filter semantics và audit field
  ownership trong specification này vào 2026-08-30.
