# SPEC-COMMERCE-04 — Atomic and Idempotent Order Creation

## Status

Draft overall — created from
`.agents/docs/plans/2026-08-15-commerce-04-atomic-idempotent-order-creation-plan.md`
per explicit user requirement. The ADR-003/ADR-004 persistence amendment was
accepted by the user on 2026-08-19; full-spec acceptance remains pending.

## Context

`POST /orders/checkout-review` (Plan 03) trả một reviewed checkout snapshot tối
thiểu. `POST /orders` hiện tại (trước Plan 04) tạo order từ toàn bộ active cart,
không có idempotency, không semantic-compare với snapshot đã review, dùng
class-level `@Transactional` khiến transaction proxy mở transaction trước khi
locks được acquired, và `InventoryService` tự hoàn reservation `ACTIVE` sau 15
phút — trái với v1 COD allocation semantics (xem ADR-002 và plan).

## Current Behavior and Evidence

- Relevant flow: `OrderController.createOrder` → `OrderService.createOrder`
  (class-level `@Transactional`) → `CartRepository.findByUserIdAndStatus` →
  collect lock keys từ toàn bộ cart items → `lock.tryLock(5, 10, SECONDS)` →
  `TransactionTemplate.execute(doCreateOrder)` → validate cart state → process
  toàn bộ cart items → voucher validate/apply → build order → clear toàn bộ
  cart → save order/history → `InventoryService.reserveStock` (tạo reservation
  `ACTIVE` + `expiresAt = now + 15min`).
- `InventoryService.reclaimExpiredReservations` `@Scheduled(fixedRate = 60000)`
  tự hoàn stock cho reservation `ACTIVE` hết hạn.
- Existing contract/spec: SPEC-COMMERCE-02 (accepted), SPEC-COMMERCE-03
  (accepted), ADR-002 (accepted), PLAN-COMMERCE-04.
- Baseline command: `./mvnw -Dtest="OrderServiceTest,*Inventory*" test` từ
  `server/`.
- Baseline result: COMPILE FAIL (pre-existing) — `OrderServiceTest` dùng
  `getOrderId()/getOrderStatus()/getTotalCheckout()/getTotalDiscount()` mà
  commit `fb3cfe3` (DTO rename) đã loại bỏ; test chưa cập nhật theo.

## Goal

- Tạo order COD từ reviewed checkout snapshot trong một transaction thật sự bắt
  đầu sau distributed locks.
- Chống retry/double-click bằng idempotency key + request hash.
- Bắt review lại khi bất kỳ dữ liệu ảnh hưởng order thay đổi (semantic-compare
  với snapshot đã review trong transaction).
- Không còn reservation tự hoàn sau 15 phút; allocation v1 là `COMMITTED` không
  auto-expire.

## Requirements

### R-C04-01: Idempotent Request/Replay

- `Idempotency-Key` header bắt buộc, phải parse được UUID.
- Request hash SHA-256 lowercase từ canonical request representation gồm
  contract version `create-order:v1`, current user ID, normalized reviewed
  checkout expectation (sorted SKU IDs, quantities, unit prices, line totals,
  voucher code, eligible subtotal, subtotal, discount, shipping fee, total),
  address mode + normalized address values (trim text, không phụ thuộc JSON
  property order), và `COD`.
- Replay lookup `(userId, idempotencyKey)` trước khi đọc active cart:
  - Tìm thấy và hash bằng → trả cùng order response/status thành công.
  - Tìm thấy nhưng hash khác → `409 IDEMPOTENCY_KEY_REUSED`.
- Recheck idempotency trong transaction sau locks; concurrent insert chạm
  unique constraint thì reload order: hash giống → replay, hash khác → 409,
  không tìm thấy sau bounded retry → propagate safe system error, không loop
  vô hạn.
- Không log full address/hash input.

### R-C04-02: Lock-Before-Transaction Ordering

- Loại class-level `@Transactional` khỏi `OrderService`; `createOrder` public
  không bị transaction proxy mở transaction trước locks.
- Read/lifecycle methods khác annotate riêng theo nhu cầu.
- Chỉ `TransactionTemplate.execute(...)` bao phần database mutation sau khi
  locks đã acquired.
- Lock keys: `lock:user-order:<userId>`, `lock:product-sku:<skuId>` cho selected
  IDs, `lock:voucher:<NORMALIZED_CODE>` nếu có voucher; deduplicate và sort
  lexicographically.
- Acquire bằng Redisson watchdog overload `tryLock(waitTime, TimeUnit)`; không
  truyền lease 10 giây. Lock timeout → release locks đã lấy theo reverse order
  và trả `SYSTEM_BUSY`/503. `finally` chỉ unlock lock do current thread giữ.

### R-C04-03: Authoritative Re-computation

- Trong transaction: reload active cart và selected items, derive sorted
  selected SKU IDs từ reviewed items, xác nhận selection thuộc active cart và
  reload authoritative SKU/product state.
- Recompute full checkout review bằng logic Plan 03 và voucher evaluator Plan
  02. Không dùng client quantity/price/discount/shipping/total làm nguồn tính
  hoặc persistence.
- Reviewed monetary fields là untrusted expectations chỉ dùng để compare;
  không nhận userId từ client.

### R-C04-04: Atomic Order/Stock/Voucher/Cart Mutation

- Semantic-compare reviewed expectation với authoritative review mới trong
  transaction:
  - Exact selected SKU set, cart quantity và normalized monetary values bằng
    `BigDecimal.compareTo` (không phụ thuộc scale).
  - Bất kỳ unit price change nào, kể cả tăng hoặc giảm, đều mismatch.
  - SKU/product không còn sellable hoặc stock không còn đủ requested quantity
    đều mismatch; raw stock count đổi nhưng vẫn đủ thì không mismatch.
  - Voucher identity/applicability, eligible subtotal, discount, shipping fee,
    subtotal hoặc total thay đổi đều mismatch; voucher counters đổi nhưng kết
    quả áp dụng và monetary outcome không đổi thì không mismatch.
  - `canPlaceOrder=false` luôn mismatch.
  - Mismatch → rollback và trả `409 CHECKOUT_CHANGED` với latest
    `CheckoutResponse` trong `ApiResult.result`; không commit mutation.
  - Match → các bước sau vẫn dùng authoritative recomputed checkout.
- Order/order items/discount/shipping/total luôn tạo từ authoritative recompute.
- Atomic mutation (cùng transaction): conditional decrement stock
  `stock >= quantity` (updated count khác 1 → insufficient và rollback),
  stock movement `SALE_OUT` tham chiếu order; `OrderItem` là nguồn committed
  quantity, không tạo inventory reservation/allocation riêng. Voucher atomic
  increment used count có guard max uses + per-user recheck từ redemption
  `REDEEMED`, xóa
  đúng selected cart items, cart còn item giữ `ACTIVE` / trống chuyển
  `COMPLETED`.
- Flush tại điểm cho phép bắt constraint violation trong transaction; không
  catch rồi commit state dở dang.
- Chỉ selected items bị xóa khỏi cart.

### R-C04-05: Durable Stock/Redemption Semantics

- Không tồn tại inventory reservation/allocation riêng cho committed COD order.
  `OrderItem` là nguồn SKU/quantity đã commit; `StockMovement` ghi `SALE_OUT`
  tham chiếu order. Atomic conditional decrement mới là guard chống oversell.
- Plan 05 dùng locked order-status transition làm exact-once guard khi hoàn stock
  toàn order, đọc quantity từ `OrderItem` và ghi `ORDER_CANCEL_RETURN` trong cùng
  transaction.
- Voucher redemption là detailed usage ledger duy nhất, liên kết voucher, user,
  order; status `REDEEMED` hoặc `REVERSED`; unique order redemption; tạo
  `REDEEMED` cùng transaction order. Không dùng bảng `voucher_user`.
- `maxPerUser` đếm redemption `REDEEMED`; `voucher.usedCount` được giữ làm atomic
  aggregate cho `maxUses`. Plan 05 mới triển khai reversal.

### R-C04-06: Address and COD Contract

- Chính xác một trong `addressId` hoặc `newAddress` được cung cấp (XOR).
- Existing address phải thuộc current user; new address phải pass validation.
- Copy snapshot vào order (recipient name, phone, full address); không chỉ giữ
  live address reference.
- `paymentMethod` chỉ nhận `COD`.
- Tạo order `PENDING`, payment `COD/UNPAID` với authoritative amounts.

### R-C04-07: Authoritative Semantic Comparison with the Reviewed Snapshot

- Request gồm `reviewedCheckout` (items: skuId, quantity, unitPrice, lineTotal;
  subtotal, eligibleSubtotal, shippingFee, discountAmount, totalAmount,
  voucher.code/applicable, canPlaceOrder), `addressId`, `newAddress`,
  `paymentMethod`.
- Selected SKU IDs derive từ `reviewedCheckout.items`; không nhận danh sách
  selection thứ hai có thể mâu thuẫn.
- `reviewedCheckout.items` non-empty; SKU IDs positive/unique, quantity positive,
  monetary fields non-negative; `canPlaceOrder=true`; voucher code normalize
  như Plan 03.
- Không nhận userId; không dùng reviewed monetary fields để ghi order.

## Acceptance Criteria

### AC-C04-01: Idempotency Lookup Before Cart Lookup

Given một request có `Idempotency-Key`,
When `POST /orders` được gọi,
Then idempotency lookup `(userId, idempotencyKey)` diễn ra trước cart lookup.

### AC-C04-02: Same Key/Same Request Replays Same Order

Given cùng key và cùng payload được submit hai lần,
When request thứ hai được xử lý,
Then cùng order được trả về, không có mutation thứ hai (stock/voucher/cart).

### AC-C04-03: Same Key/Different Request Returns 409

Given cùng key nhưng payload khác,
When request thứ hai được xử lý,
Then trả `409 IDEMPOTENCY_KEY_REUSED`, không tạo order thứ hai.

### AC-C04-04: Lock Acquisition Completes Before Transaction

Given create order được gọi,
When transaction callback chạy,
Then toàn bộ distributed locks đã được acquire trước đó; không có transaction
proxy mở transaction trước locks.

### AC-C04-05: No Fixed 10-Second Lease

Given create order được gọi,
When lock được acquire,
Then watchdog overload `tryLock(waitTime, TimeUnit)` được dùng, không truyền
fixed lease 10 giây.

### AC-C04-06: Create Recomputes and Semantic-Compares in Transaction

Given reviewed snapshot được gửi cùng request,
When transaction chạy,
Then checkout được recompute authoritative và semantic-compare với reviewed
snapshot trong transaction trước khi mutate.

### AC-C04-07: Stock/Voucher/Cart/Order Share Rollback Boundary

Given bất kỳ bước mutation nào fail,
When transaction rollback xảy ra,
Then stock, voucher, cart và order đều nằm trong cùng rollback boundary.

### AC-C04-08: Only Selected Items Removed

Given cart có nhiều item và user chọn subset,
When order được tạo,
Then chỉ selected items bị xóa khỏi cart; unselected items còn lại.

### AC-C04-09: Committed Stock Has No Reservation Row or Auto-Expire

Given order được tạo thành công,
When stock được commit,
Then stock giảm atomically, `OrderItem` và `SALE_OUT` movement được ghi, không
có inventory reservation/allocation row, `expiresAt`, hoặc scheduler tự hoàn
stock.

### AC-C04-10: Concurrency Tests Prove No Duplicate/Oversell

Given N concurrent request cùng idempotency key hoặc cùng SKU,
When xử lý xong,
Then chỉ một order/một stock decrement được tạo; không oversell.

### AC-C04-11: No Stage/Commit/Push

Given toàn bộ work hoàn tất,
Then working tree không bị stage/commit/push.

### AC-C04-12: Any Price or Order-Affecting Change Returns 409

Given bất kỳ price hoặc order-affecting state change sau review,
When create order được gọi,
Then trả `409 CHECKOUT_CHANGED` với latest review và không mutation.

### AC-C04-13: Reviewed Monetary Fields Never Persist; Raw Stock Change Does Not False-Mismatch

Given raw stock count thay đổi nhưng vẫn đủ requested quantity,
When create order được gọi,
Then không false mismatch; atomic decrement vẫn bảo vệ concurrent oversell;
reviewed monetary fields không bao giờ là nguồn persistence.

## Non-Goals

- User/admin cancellation và status transitions.
- Frontend checkout/history/admin.
- Flyway `V1` và production reset (Plan 10 sẽ tạo `V1` final từ clean schema;
  trước đó JPA update phục vụ development/test, không tạo Flyway baseline tạm).
- Reversal/`REVERSED` redemption (Plan 05).

## Edge Cases

- `Idempotency-Key` thiếu hoặc không parse được UUID → HTTP 400.
- Cả `addressId` và `newAddress` cùng cung cấp hoặc cùng thiếu → HTTP 400.
- `paymentMethod` khác `COD` → HTTP 400 (enum binding).
- `reviewedCheckout.items` rỗng, SKU ID không positive/trùng, quantity không
  positive, monetary field âm → HTTP 400.
- `canPlaceOrder=false` → luôn mismatch (`CHECKOUT_CHANGED`).
- Concurrent insert chạm unique constraint → reload order: hash giống → replay,
  hash khác → 409, không tìm thấy sau bounded retry → safe system error.
- Lock acquisition timeout một phần → release các lock đã lấy theo reverse
  order, trả `SYSTEM_BUSY` 503.
- Raw stock count đổi nhưng vẫn đủ → không mismatch; không đủ → mismatch.
- Voucher counters đổi nhưng applicability/monetary outcome không đổi → không
  mismatch.

## Domain Invariants

- Database là nguồn sự thật duy nhất cho mọi amount; reviewed snapshot chỉ là
  untrusted expectation cho semantic comparison.
- Không map monetary field từ request trực tiếp vào `Order`/`OrderItem`.
- `totalAmount >= 0`; `discountAmount <= eligibleSubtotal`; line total = database
  unit price × cart quantity.
- Committed stock dùng `OrderItem` + `StockMovement`, không có reservation tạm,
  allocation row hoặc auto-expire khi tạo order.
- Idempotency: một `(user_id, idempotency_key)` chỉ có một order.

## API Contract

### Request

```http
POST /orders
Authorization: Bearer <token>
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json
```

```json
{
  "reviewedCheckout": {
    "items": [
      {
        "skuId": 17,
        "quantity": 2,
        "unitPrice": 100000,
        "lineTotal": 200000
      }
    ],
    "subtotal": 200000,
    "eligibleSubtotal": 200000,
    "shippingFee": 30000,
    "discountAmount": 20000,
    "totalAmount": 210000,
    "voucher": {
      "code": "SUMMER",
      "applicable": true
    },
    "canPlaceOrder": true
  },
  "addressId": "uuid-or-null",
  "newAddress": null,
  "paymentMethod": "COD"
}
```

Validation:

- `Idempotency-Key` bắt buộc và phải parse được UUID.
- `reviewedCheckout.items` non-empty; SKU IDs positive/unique, quantity positive,
  monetary fields non-negative, `canPlaceOrder=true` và voucher code được
  normalize như Plan 03.
- Selected SKU IDs được derive từ `reviewedCheckout.items`; không nhận danh
  sách selection thứ hai.
- Chính xác một trong `addressId` hoặc `newAddress` được cung cấp.
- `paymentMethod` chỉ nhận `COD`.
- Reviewed monetary fields là untrusted expectations chỉ dùng để compare; không
  nhận userId và không dùng các field này để ghi order.

### Response

- Success: `ApiResult<OrderResponse>` HTTP 200 (hoặc 201 theo convention repo
  hiện tại; plan giữ HTTP 200 như controller hiện có).
- Replay: cùng response/status như order gốc.
- `409 CHECKOUT_CHANGED`: `ApiResult` với latest `CheckoutResponse` trong
  `ApiResult.result`.
- `409 IDEMPOTENCY_KEY_REUSED`: `ApiResult` với error code
  `IDEMPOTENCY_KEY_REUSED`.
- `503 SYSTEM_BUSY`: lock timeout.

### Errors

- `400` — header thiếu/sai UUID, validation DTO, address XOR, `COD` only binding.
- `401` — unauthenticated.
- `403` — address không thuộc user (không lộ dữ liệu).
- `409` — `IDEMPOTENCY_KEY_REUSED` hoặc `CHECKOUT_CHANGED` (kèm latest review).
- `503` — `SYSTEM_BUSY` (lock timeout).

## Security / Authorization

- Endpoint bắt buộc authenticated user; user resolve qua `AuthService`.
- Không nhận hoặc tin `userId`, price, subtotal từ client.
- Address phải thuộc current user; lỗi owner không lộ dữ liệu cart/address của
  user khác.
- Không log full address/hash input.

## Data / Persistence Considerations

- `Order`: `idempotencyKey` UUID/string canonical, `requestHash` SHA-256
  lowercase, unique `(user_id, idempotency_key)`, `@Version`, voucher FK
  nullable + voucher code snapshot, address snapshot fields, created/updated
  timestamps theo convention repo, payment method/status COD/UNPAID.
- Không có bảng `inventory_reservation`; `OrderItem` giữ committed quantity và
  `StockMovement.referenceId` tham chiếu order.
- `VoucherRedemption`: liên kết voucher, user, order; status `REDEEMED`/
  `REVERSED`; unique order redemption; tạo `REDEEMED` cùng transaction order;
  là nguồn đếm `maxPerUser`. Không có bảng `voucher_user`.
- Trước Plan 10 dùng JPA update phục vụ development/test; không tạo Flyway
  baseline tạm.
- **Schema upgrade prerequisite (bắt buộc trước khi chạy Plan 04 trên database
  đã có dữ liệu)**: Plan 04 thay đổi schema không an toàn với
  `ddl-auto: update` — `Order` thêm `idempotency_key`/`request_hash`/
  `payment_status` dạng NOT NULL, bỏ bảng `inventory_reservation` và
  `voucher_user`, thêm unique constraints và bảng `voucher_redemption`.
  Hibernate update không tự backfill/drop table/constraint cũ; với database cũ,
  schema có thể giữ cấu trúc obsolete. Do đó database dùng cho Plan 04
  phải được **reset sạch** (drop/recreate) trước khi chạy; mọi dữ liệu
  development/test cũ bị xóa. Sau Plan 10, Flyway `V1` thay thế prerequisite
  này bằng migration chính thức.

## Verification Strategy

### Unit

- Header UUID thiếu/sai.
- Address XOR validation.
- Reviewed snapshot DTO validation; item order được normalize và duplicate SKU
  bị reject.
- Request hash deterministic, không phụ thuộc item order/JSON order hoặc
  equivalent `BigDecimal` scale.
- Replay same key/same hash trả existing order trước cart lookup.
- Same key/different hash trả 409.
- Lock keys deduplicated/sorted; partial lock failure release đúng.
- Transaction callback chỉ được gọi sau locks.
- Watchdog overload được dùng, không fixed lease.
- Reviewed snapshot match cho phép create nhưng order amounts vẫn lấy từ
  authoritative recomputation.
- Bất kỳ SKU price change nào, kể cả tăng hoặc giảm, trả 409 với latest review.
- Selection/quantity/sellability/stock sufficiency/voucher outcome/shipping hoặc
  total thay đổi trả 409 và không mutation.
- Raw stock count thay đổi nhưng vẫn đủ requested quantity không gây false
  mismatch; atomic decrement vẫn bảo vệ concurrent oversell.
- Client sửa reviewed price/discount/total không làm thay đổi persisted order và
  mismatch với authoritative review khi giá trị khác.
- Partial checkout chỉ xử lý selected lines.

### MVC / Component

- Header/nested reviewed-snapshot validation và `409` response chứa latest
  checkout.
- COD-only enum binding.
- Owner address error không lộ dữ liệu.

### Integration

- Success lưu order/items/history/`SALE_OUT` movement/redemption và giảm stock;
  không tạo inventory reservation hoặc `voucher_user` row.
- Unselected cart items còn lại, cart vẫn active.
- Last selected item làm cart completed.
- Failure sau stock decrement rollback stock/order/cart/voucher.
- N concurrent same idempotency key tạo một order/một stock decrement.
- Same key/different payload không tạo order thứ hai.
- Concurrent orders không oversell.
- Không có scheduler tự hoàn stock sau thời hạn cũ.

### Static / Build / Runtime

- `./mvnw -Dtest="OrderServiceTest,*OrderController*,OrderIT,*Inventory*" test`
- `./mvnw test`

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
|---|---|---|---|
| AC-C04-01 | `OrderServiceTest` replay-before-cart | UAT 1 | PLANNED |
| AC-C04-02 | `OrderServiceTest` replay same key/hash | UAT 1 | PLANNED |
| AC-C04-03 | `OrderServiceTest` same key/different hash 409 | UAT 2 | PLANNED |
| AC-C04-04 | `OrderServiceTest` lock-before-transaction | — | PLANNED |
| AC-C04-05 | `OrderServiceTest` watchdog overload | — | PLANNED |
| AC-C04-06 | `OrderServiceTest` + `OrderIT` recompute/compare | UAT 3 | PLANNED |
| AC-C04-07 | `OrderIT` rollback boundary | UAT 4 | PLANNED |
| AC-C04-08 | `OrderIT` partial checkout cart remains | UAT 7 | PLANNED |
| AC-C04-09 | `OrderIT` order item + SALE_OUT, no reservation | UAT 5 | PLANNED |
| AC-C04-10 | `OrderIT` concurrency tests | UAT 8 | PLANNED |
| AC-C04-11 | `git status` | — | PLANNED |
| AC-C04-12 | `OrderServiceTest` price change 409 | UAT 3 | PLANNED |
| AC-C04-13 | `OrderServiceTest` + `OrderIT` authoritative amounts | UAT 6 | PLANNED |

## Acceptance Scenarios

1. Submit cùng key/payload hai lần → cùng order, không mutation thứ hai.
2. Reuse key với payload khác → 409 an toàn.
3. Thay đổi authoritative checkout state sau review → `CHECKOUT_CHANGED` trả
   latest review, không mutation.
4. Tăng rồi giảm giá một SKU sau các lần review riêng → cả hai đều cần review
   mới trước khi tạo order.
5. Đổi voucher applicability/discount hoặc shipping fee → cần review mới.
6. Raw stock đổi nhưng vẫn đủ → create vẫn chạy; giảm dưới requested quantity →
   checkout bị reject.
7. Order cho subset → unselected cart items còn lại.
8. Double-click/concurrent submit trong môi trường PostgreSQL staging-like →
   inspect order, stock, order items, stock movement, redemption, cart state.

## Open Questions

- Không còn material question: plan khóa API contract, lock API và domain
  semantics; ADR-002 và SPEC-COMMERCE-02/03 đã khóa các contract phụ thuộc.
