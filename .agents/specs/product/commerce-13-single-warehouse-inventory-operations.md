# SPEC-COMMERCE-13 — Vận hành tồn kho một kho và lịch sử biến động

## Status

Draft

Tài liệu được tạo ngày 2026-09-02 từ yêu cầu hiện tại của user: giữ
`Inventory` và `StockMovement`, không thêm `reserved`, reservation table hoặc
reservation lifecycle. Spec cần được user review và ACCEPT trước khi triển khai
các API quản lý kho mới.

### P0 Delivery Slice — Accepted

Accepted by the user on 2026-09-02. This acceptance authorizes only the
`R-C13-P0-*` requirements and `AC-C13-P0-*` acceptance criteria below. The
broader production-hardening target in this Draft remains unaccepted.

User yêu cầu ngày 2026-09-02 implement một P0/L1 happy-path slice thay vì toàn
bộ production-hardening target trong tài liệu này. Chỉ các requirement và
acceptance criterion mang prefix `R-C13-P0`/`AC-C13-P0` dưới đây được đề nghị
cho implementation hiện tại. Các requirement tổng thể còn lại tiếp tục là định
hướng tương lai và không được xem là implementation authorization.

#### P0 decisions

- Giữ compatibility của Product/SKU request field `stock`:
  - SKU create dùng giá trị này làm opening balance;
  - SKU update/reconcile dùng nó làm desired on-hand và phải đi qua audited
    adjust-to command thay vì blind `setOnHand`.
- P0 tận dụng schema `stock_movement` hiện có. `reason` được type thành Java enum
  persisted dạng string; chưa thêm `balanceBefore`, `balanceAfter`,
  `referenceType`, note hoặc admin-idempotency persistence.
- P0 không thêm held/allocation Inventory projection. Admin tiếp tục dùng Order
  list/detail để xem order đã consume stock.
- P0 mở một Inventory admin controller cho adjustment và movement history. Khi
  slice này được ACCEPT, nó supersede duy nhất Commerce-12 R-C12-06/non-goal ở
  phần “không có Inventory controller/new inventory adjustment API”; các quyết
  định ownership, no-reserved và order exact-once của Commerce-12/ADR-003/
  ADR-005 vẫn giữ nguyên.

#### R-C13-P0-01 — Typed movement vocabulary

- `StockMovement.reason` dùng enum persisted dạng string với tối thiểu:
  `OPENING_BALANCE`, `PURCHASE_IN`, `DAMAGE_OUT`, `MANUAL_ADJUSTMENT_IN`,
  `MANUAL_ADJUSTMENT_OUT`, `SALE_OUT`, `ORDER_CANCEL_RETURN`.
- `SALE_OUT`, `ORDER_CANCEL_RETURN` và các reason system-only không được client
  admin chọn tùy ý.
- Existing rows `SALE_OUT`/`ORDER_CANCEL_RETURN` phải tiếp tục đọc được.

#### R-C13-P0-02 — Opening balance và audited adjust-to

- SKU create với stock dương tạo Inventory và `OPENING_BALANCE` cùng transaction;
  stock bằng 0 không ghi movement delta 0.
- Existing Product/SKU update/reconcile desired stock phải lock/read current
  Inventory, tính delta và ghi `MANUAL_ADJUSTMENT_IN/OUT` cùng transaction.
- Desired stock bằng current balance là no-op; không ghi movement.
- Không dùng stale client balance để tự tính signed delta.

#### R-C13-P0-03 — Admin increase/decrease

- Chỉ ADMIN gọi được adjustment endpoint.
- Request dùng `operation=INCREASE|DECREASE`, positive quantity và reason hợp lệ
  cho direction; client không gửi signed delta.
- Increase guard integer overflow; decrease guard `onHand >= quantity`.
- Balance mutation và movement commit/rollback cùng transaction.
- Response trả current authoritative on-hand và movement vừa tạo.

#### R-C13-P0-04 — Basic movement history

- Chỉ ADMIN xem movement history nội bộ.
- History hỗ trợ pagination, optional `skuId`, optional reason và ordering ổn
  định `createdAt DESC, id DESC`.
- Response dùng DTO, không serialize trực tiếp JPA entity/lazy relationships.

#### R-C13-P0-05 — Preserve L1 stock correctness

- Order creation tiếp tục atomic decrement và ghi `SALE_OUT`.
- Cancellation tiếp tục exact-once restore từ `OrderItem` và ghi
  `ORDER_CANCEL_RETURN`.
- Admin mutation cạnh tranh với order/cancellation trên cùng SKU không được lost
  update hoặc làm stock âm.
- Missing Inventory, non-positive quantity, insufficient decrease, overflow và
  failure khi ghi movement đều fail closed, không để partial balance/movement.

#### AC-C13-P0-01 — Opening balance được audit

Given ADMIN tạo SKU với stock 10,
when transaction commit,
then Inventory on-hand bằng 10 và có đúng một `OPENING_BALANCE +10`; stock 0
không tạo movement delta 0.

Maps to R-C13-P0-01 and R-C13-P0-02.

#### AC-C13-P0-02 — Admin adjustment thay đổi balance và movement cùng nhau

Given một Inventory hợp lệ,
when ADMIN increase hoặc decrease với quantity/reason hợp lệ,
then balance đổi đúng signed delta, một movement có đúng actor/reason được ghi và
response trả current balance; insufficient/overflow không để partial state.

Maps to R-C13-P0-03 and R-C13-P0-05.

#### AC-C13-P0-03 — Existing stock update có audit

Given SKU có current on-hand 10,
when existing admin update/reconcile gửi desired stock 15,
then balance thành 15 và ghi `MANUAL_ADJUSTMENT_IN +5`; gửi lại desired 15 không
tạo movement mới.

Maps to R-C13-P0-02.

#### AC-C13-P0-04 — Admin xem basic movement history

Given SKU có opening, adjustment, sale và cancellation movements,
when ADMIN query history theo SKU/reason,
then page trả đúng filter với ordering ổn định; unauthenticated/non-admin không
được truy cập.

Maps to R-C13-P0-01 and R-C13-P0-04.

#### AC-C13-P0-05 — Không regression order và không lost update

Given order sale/cancellation hoặc một order sale chạy cạnh tranh với admin
adjustment trên cùng SKU,
when các transaction kết thúc,
then current balance phản ánh chính xác các command đã commit, không âm, không
lost update và mỗi mutation thành công có movement tương ứng.

Maps to R-C13-P0-05.

#### P0 non-goals

- Persisted idempotency cho admin adjustment retry/double-click.
- `balanceBefore`/`balanceAfter`, `referenceType`, note hoặc metadata JSON.
- Held/allocation Inventory API.
- Reconciliation report/job, metrics, alerts và DB-level immutable trigger.
- Frontend Inventory UI, multi-warehouse, return/RMA hoặc purchase-order.
- Full production-readiness claim; P0 vẫn yêu cầu manual UAT và ghi rõ known
  retry limitation.

## Context

Commerce-12 và ADR-005 đã chuyển quyền sở hữu số lượng tồn hiện tại sang
`Inventory.onHand`. ADR-003 đã chốt rằng order COD trừ stock ngay khi tạo,
`OrderItem` lưu số lượng đã cấp cho order, `StockMovement` ghi lịch sử
`SALE_OUT`/`ORDER_CANCEL_RETURN`, và hệ thống không cần một reservation model
thứ hai.

Implementation hiện tại đã bảo vệ order creation và cancellation, nhưng phần
quản lý kho của admin chưa hoàn chỉnh:

- tạo/cập nhật/reconcile SKU có thể đặt `Inventory.onHand` trực tiếp;
- các thay đổi trực tiếp này chưa ghi `StockMovement`;
- chưa có lệnh nhập kho, xuất điều chỉnh hoặc kiểm kê độc lập;
- chưa có API xem lịch sử biến động;
- thay đổi tuyệt đối bằng `setOnHand` có thể ghi đè một thay đổi đồng thời nếu
  không có lock/version/atomic command phù hợp.

Phạm vi mục tiêu là một cửa hàng bán máy vi tính với đúng một kho. Không cần
warehouse allocation, multi-warehouse transfer hoặc reservation lifecycle.

## Goal

Module Inventory phải trả lời được ba câu hỏi kinh doanh:

1. **Còn bao nhiêu hàng có thể bán?**
   `Inventory.onHand` là số dư sellable hiện tại và là source of truth duy nhất.
2. **Order nào đã chiếm stock?**
   Suy ra từ `OrderItem` kết hợp trạng thái order. Stock đã bị trừ khỏi
   `onHand` ngay khi order được tạo; không lưu thêm `reserved`.
3. **Vì sao stock thay đổi, ai thực hiện và tham chiếu nghiệp vụ nào?**
   `StockMovement` là append-only audit ledger cho mọi thay đổi số dư sau thời
   điểm mở sổ/cutover.

Hệ thống phải hỗ trợ an toàn trong một database transaction:

```text
Get available stock
Consume stock for order
Restore stock for order cancellation

Create opening balance
Increase stock
Decrease stock
Adjust stock after physical count
View stock movement
```

Không có các use case:

```text
Reserve stock
Release reservation
Commit reservation
Expire reservation
```

Lifecycle tương ứng là lifecycle của Order, không phải Inventory reservation:

```text
Order created (PENDING)
  -> trừ Inventory.onHand
  -> ghi SALE_OUT

PENDING/CONFIRMED -> CANCELLED
  -> cộng lại Inventory.onHand đúng một lần
  -> ghi ORDER_CANCEL_RETURN đúng một lần

CONFIRMED -> SHIPPING -> DELIVERED
  -> không trừ stock thêm lần nữa
```

## Requirements

### R-C13-01 — Mô hình số dư một kho

- `Inventory.onHand` là số lượng còn có thể bán trong mô hình decrement-at-order.
- `available = onHand`; không có cột `reserved`.
- Mỗi SKU có đúng một Inventory row theo shared identity của Commerce-12.
- `onHand` không null, không âm và không được overflow kiểu integer.
- Catalog, cart, checkout, order và admin đọc số dư qua Inventory-owned boundary.
- `StockMovement` không được dùng thay `Inventory.onHand` trong request bán hàng
  và không trở thành current-balance source thứ hai.

### R-C13-02 — Stock đã cấp cho order

- Tạo order trừ `Inventory.onHand` bằng atomic conditional update và ghi
  `SALE_OUT` trong cùng transaction.
- `SALE_OUT` tham chiếu order đã nhận stock và quantity âm bằng đúng số lượng
  `OrderItem` tương ứng.
- Các order `PENDING` và `CONFIRMED` được xem là đang giữ hàng trong kho về mặt
  vận hành; danh sách/số lượng này được suy ra từ `OrderItem + Order.status`,
  không persist thành `reserved`.
- Chuyển order sang `SHIPPING` hoặc `DELIVERED` không thay đổi stock lần nữa.
- Payment success không thay đổi stock lần nữa trong mô hình này.

### R-C13-03 — Hủy order và hoàn stock đúng một lần

- Chỉ transition hợp lệ sang `CANCELLED` mới được hoàn stock.
- Số lượng hoàn lấy từ persisted `OrderItem`, không lấy từ cart hoặc catalog.
- Hoàn stock và ghi `ORDER_CANCEL_RETURN` trong cùng locked order transaction.
- Retry hoặc concurrent cancellation không được cộng stock hay ghi movement
  lần thứ hai.
- Order đã `SHIPPING` hoặc `DELIVERED` không đi qua cancellation-return flow;
  return/refund sau giao hàng là một nghiệp vụ riêng.

### R-C13-04 — Opening balance và thay đổi tồn kho của admin

- Tạo SKU với stock dương tạo Inventory và một movement `OPENING_BALANCE` trong
  cùng transaction; stock bằng 0 không cần movement delta 0.
- Admin increase nhận một quantity dương và tạo movement dương với reason phù
  hợp, tối thiểu `PURCHASE_IN` hoặc `MANUAL_ADJUSTMENT_IN`.
- Admin decrease nhận một quantity dương, dùng atomic guard để không làm stock
  âm và tạo movement âm, tối thiểu `DAMAGE_OUT` hoặc
  `MANUAL_ADJUSTMENT_OUT`.
- Kiểm kê có thể nhận desired on-hand, nhưng service phải khóa/kiểm soát version,
  tính delta từ số dư authoritative hiện tại và ghi movement điều chỉnh; không
  blind-save một số tuyệt đối từ stale request.
- Mọi lệnh admin phải lưu actor, reason và note/reference đủ để audit.
- Product/SKU metadata update không được âm thầm thay stock mà không tạo
  movement. Existing API field `stock` phải được chuyển qua audited adjustment
  hoặc bị loại khỏi update flow bằng một contract được version hóa rõ ràng.

### R-C13-05 — StockMovement là append-only audit ledger

- Mỗi thay đổi `onHand` sau opening/cutover phải có đúng một movement tương ứng
  cho mỗi SKU trong cùng transaction.
- Movement tối thiểu lưu SKU, signed delta, typed reason, reference type/id,
  actor khi có, note khi cần và timestamp.
- Movement của admin phải có actor; movement hệ thống phải có reference nghiệp
  vụ hoặc system source rõ ràng.
- Movement đã commit không được update hoặc hard-delete. Sửa sai bằng một
  movement bù trừ mới.
- Delta bằng 0 bị từ chối.
- Nếu update balance hoặc insert movement thất bại, toàn transaction rollback.
- Ledger chỉ reconcile được từ một opening/cutover balance được định nghĩa rõ;
  không giả định lịch sử demo cũ là một ledger đầy đủ.

Các reason tối thiểu trong scope:

```text
OPENING_BALANCE
PURCHASE_IN
SALE_OUT
ORDER_CANCEL_RETURN
DAMAGE_OUT
MANUAL_ADJUSTMENT_IN
MANUAL_ADJUSTMENT_OUT
```

### R-C13-06 — Concurrency và idempotency

- Atomic database update là correctness boundary cuối cùng; Redis/distributed
  lock chỉ là coordination aid.
- Concurrent orders không được oversell hoặc làm stock âm.
- Concurrent admin adjustment và order sale trên cùng SKU không được lost
  update.
- Concurrent admin adjustments phải serialize hoặc dùng atomic delta update.
- Lệnh admin có một idempotency key/request ID. Cùng key và cùng payload replay
  cùng kết quả; cùng key khác payload trả conflict.
- Increase phải guard integer overflow; decrease phải guard insufficient stock.
- Idempotency record/result, Inventory update và StockMovement commit atomically.

### R-C13-07 — API quản lý kho và phân quyền

- Chỉ ADMIN được increase, decrease, physical-count adjustment và xem movement
  có thông tin actor/note nội bộ.
- API mutation nhận quantity dương cùng operation/reason thay vì yêu cầu client
  tự gửi signed delta mơ hồ.
- API movement hỗ trợ pagination và filter tối thiểu theo SKU, reason,
  reference, actor và khoảng thời gian; thứ tự ổn định `createdAt DESC, id DESC`.
- API trả số dư mới sau mutation để UI không phải tự cộng trừ local.
- Customer API không được expose note nội bộ hoặc thông tin actor.
- Missing SKU/Inventory, invalid quantity, insufficient stock, duplicate
  idempotency conflict và concurrent modification có error contract ổn định.

### R-C13-08 — Khả năng vận hành production một kho

- Database có constraint cho non-negative balance, non-zero movement delta và
  các trường bắt buộc theo movement type trong phạm vi DB hỗ trợ hợp lý.
- Có index phục vụ movement history theo `(sku_id, created_at, id)` và lookup
  theo business reference/idempotency key.
- Trước khi dùng dữ liệu staging/production không thể xóa, schema change phải
  đi qua migration versioned được review; Hibernate `ddl-auto: update` không
  phải production migration strategy.
- Cutover phải định nghĩa opening balance và mốc bắt đầu reconcile ledger.
- Có metric/log cho insufficient stock, missing Inventory invariant, movement
  write failure và adjustment conflict; không log note nhạy cảm tùy tiện.
- Có thủ tục reconciliation định kỳ hoặc theo yêu cầu để phát hiện SKU thiếu
  Inventory, balance âm và movement/reference bất thường. Reconciliation không
  tự sửa dữ liệu nếu chưa có lệnh adjustment được audit.

## Acceptance Criteria

### AC-C13-01 — Available stock có một source of truth

Given một SKU active có Inventory,
when catalog, cart, checkout và admin đọc stock,
then tất cả dùng `Inventory.onHand`, `available = onHand`, và không tồn tại
`reserved` hoặc reservation persistence.

Maps to R-C13-01.

### AC-C13-02 — Order consume stock atomically

Given nhiều order cạnh tranh vượt quá `onHand`,
when chúng được tạo,
then chỉ quantity được atomic update chấp nhận mới tạo order/movement,
`onHand` không âm và mỗi order thành công có đúng `SALE_OUT` tương ứng.

Maps to R-C13-02 and R-C13-06.

### AC-C13-03 — Có thể xác định order đang giữ hàng mà không reserved

Given các order ở `PENDING`, `CONFIRMED`, `SHIPPING`, `DELIVERED` và
`CANCELLED`,
when admin xem allocation/held projection,
then chỉ persisted `OrderItem` của các trạng thái được định nghĩa là đang giữ
hàng được trả về và không có counter reservation phải đồng bộ.

Maps to R-C13-02.

### AC-C13-04 — Cancellation hoàn stock đúng một lần

Given một order cancellable,
when cancellation được retry hoặc chạy cạnh tranh,
then chỉ một status transition hoàn `OrderItem.quantity`, chỉ một
`ORDER_CANCEL_RETURN` mỗi item được ghi và mọi side effect commit/rollback cùng
nhau.

Maps to R-C13-03 and R-C13-05.

### AC-C13-05 — Opening balance có audit

Given admin tạo SKU với stock dương,
when transaction commit,
then Inventory có đúng số dư ban đầu và một `OPENING_BALANCE` cùng SKU, delta,
actor/system source; nếu một bước fail thì không còn dữ liệu một phần.

Maps to R-C13-04 and R-C13-05.

### AC-C13-06 — Admin increase/decrease an toàn

Given admin gửi một adjustment hợp lệ,
when command commit,
then `onHand` đổi đúng delta, movement lưu đúng direction/reason/actor/reference
và response trả số dư mới; insufficient decrease hoặc overflow không thay đổi
balance/movement.

Maps to R-C13-04, R-C13-05, R-C13-06, and R-C13-07.

### AC-C13-07 — Admin retry không điều chỉnh hai lần

Given cùng idempotency key và payload được gửi lại,
when request đầu đã commit,
then request sau replay kết quả mà không đổi stock hoặc ghi movement lần hai;
cùng key khác payload trả conflict.

Maps to R-C13-06 and R-C13-07.

### AC-C13-08 — Sale và admin adjustment không lost update

Given order sale và admin adjustment chạy đồng thời trên cùng SKU,
when cả hai kết thúc,
then số dư cuối phản ánh chính xác các command đã commit, không âm, không lost
update và ledger có movement tương ứng với từng command thành công.

Maps to R-C13-05 and R-C13-06.

### AC-C13-09 — Movement history đủ để audit

Given SKU có opening, sale, cancellation và manual adjustments,
when admin filter movement history,
then kết quả pageable có thứ tự ổn định, signed delta, reason, reference, actor,
note/timestamp phù hợp và không expose dữ liệu nội bộ qua customer API.

Maps to R-C13-05 and R-C13-07.

### AC-C13-10 — Reconciliation phát hiện sai lệch nhưng không tự che giấu

Given một invariant bị phá vỡ hoặc ledger không có opening baseline,
when reconciliation chạy,
then hệ thống report sai lệch/mốc không thể reconcile và không tự sửa balance
hoặc tạo movement giả mà không có audited command.

Maps to R-C13-05 and R-C13-08.

## Non-Goals

- `reserved`, reservation table, reservation expiry hoặc payment hold.
- Multi-warehouse, warehouse transfer, bin/location hoặc serial-number stock.
- Backorder, partial allocation, partial cancellation hoặc partial fulfillment.
- Purchase-order/supplier management đầy đủ.
- Return/refund/RMA sau delivery; nếu hàng được nhập lại kho, một spec riêng phải
  quyết định điều kiện restock và liên kết payment/refund.
- Event sourcing: StockMovement là audit ledger, còn `Inventory.onHand` vẫn là
  current-balance source phục vụ request.
- Forecasting, reorder point, low-stock notification hoặc automatic purchasing.

## Edge Cases

- SKU/Inventory không tồn tại hoặc invariant một SKU–một Inventory bị phá vỡ.
- Tạo opening balance bằng 0, âm, null hoặc vượt giới hạn integer.
- Increase/decrease quantity bằng 0, âm hoặc overflow.
- Decrease lớn hơn `onHand`.
- Hai order mua những đơn vị cuối cùng của cùng SKU.
- Hai admin cùng adjustment một SKU.
- Admin adjustment cạnh tranh với order sale hoặc cancellation return.
- Admin submit lại do timeout/network retry.
- Cùng idempotency key nhưng payload khác.
- Movement insert fail sau khi balance update và ngược lại.
- Product/SKU update mang stale absolute stock.
- Retry và concurrent order cancellation.
- SKU inactive/soft-deleted còn stock lịch sử: không bán nhưng history vẫn xem
  được; policy điều chỉnh/disposition phải explicit trong admin command.
- Order `SHIPPING`/`DELIVERED` bị yêu cầu cancel.
- Payment success đến sau cancellation hoặc được gửi lặp: không tạo movement
  stock trong mô hình decrement-at-order.
- Movement có reference trỏ tới order không tồn tại hoặc sai loại reference.
- Nhiều movement có cùng timestamp: pagination vẫn ổn định nhờ `id` tie-breaker.
- Legacy/demo movements tồn tại trước opening/cutover baseline.

## Domain Invariants

- Một SKU có đúng một current sellable balance: `Inventory.onHand`.
- `onHand >= 0`; không có `reserved`.
- Order creation là thời điểm stock rời khỏi available balance.
- Payment/delivery không trừ stock lần hai.
- Cancellation hợp lệ hoàn stock đúng một lần từ persisted `OrderItem`.
- Mọi balance mutation sau opening/cutover có movement cùng transaction.
- Movement là immutable audit record; correction dùng compensating movement.
- Tổng opening balance và các delta sau đúng mốc cutover phải reconcile về
  current `onHand`; nếu thiếu baseline thì phải report là không đủ dữ liệu.
- Authorization và idempotency là một phần của correctness cho admin mutation.

## API Contract

API path cuối cùng sẽ được chốt trong implementation plan, nhưng behavior contract
tối thiểu là:

### Request

```text
POST /admin/inventory/{skuId}/adjustments

operation: INCREASE | DECREASE
quantity: positive integer
reason: allowed reason for operation
note: required for manual/correction reasons
idempotencyKey: required
```

Physical count có thể dùng endpoint riêng hoặc operation riêng nhận
`desiredOnHand` cùng expected version. Client không tự tính signed delta dựa trên
một stock snapshot cũ.

### Response

```text
skuId
previousOnHand
currentOnHand
movementId
operation
quantity
reason
reference
createdBy
createdAt
```

Movement history là pageable response với deterministic ordering.

### Errors

- invalid quantity/reason/note;
- SKU hoặc Inventory không tồn tại;
- insufficient stock;
- on-hand overflow;
- idempotency key reused with different payload;
- adjustment conflict/concurrent modification;
- unauthenticated/forbidden.

## Security / Authorization

- Mutation và internal movement history chỉ dành cho ADMIN.
- Service boundary tiếp tục enforce `hasRole('ADMIN')`; controller security chỉ
  là lớp bổ sung, không phải lớp duy nhất.
- Actor lấy từ authenticated principal qua `AuthService`, không tin user ID do
  client gửi.
- Note được giới hạn độ dài, trim/validate và không dùng làm nơi lưu dữ liệu
  thanh toán hoặc thông tin nhạy cảm.

## Data / Persistence Considerations

- Giữ `inventory(sku_id PK/FK, on_hand NOT NULL CHECK on_hand >= 0)`.
- Evolve `stock_movement` để reason/reference có contract type-safe và hỗ trợ
  idempotency/audit; không persist một balance counter thứ hai.
- Có thể lưu `balance_before`/`balance_after` để audit nhanh nếu mutation được
  serialize tại Inventory row; nếu thêm, DB/application phải đảm bảo
  `balance_after - balance_before = change_qty`.
- Mutation balance, movement và idempotency evidence nằm trong cùng transaction.
- Versioned migration và backup/rollback procedure là bắt buộc trước production
  data cutover.

## Verification Strategy

### Unit

- Reason/operation validation và signed delta mapping.
- Opening balance, increase, decrease, physical-count delta và overflow.
- Same-key replay/different-payload conflict.
- Actor/reference/note mapping và không ghi movement khi command fail.

### MVC / Component

- ADMIN success; unauthenticated 401; non-admin 403.
- Request validation, error envelope, pagination/filter contract.
- Customer endpoints không expose movement audit nội bộ.

### Integration

- Atomic balance + movement commit/rollback trên PostgreSQL.
- Constraints, indexes và immutable-write policy phù hợp.
- Concurrent sale vs sale, admin vs admin, admin vs sale/cancellation.
- Idempotent retry không double adjustment.
- Order cancellation exact-once tiếp tục PASS.
- Movement pagination/filter và cutover reconciliation.

### Static / Build / Runtime

- Focused Inventory/Product/Order suites.
- Server format/static checks.
- GitNexus impact trước symbol edits và detect-changes trước commit.
- Schema inspection trên PostgreSQL thực, không suy luận chỉ từ entity.

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
| --- | --- | --- | --- |
| AC-C13-01 | Inventory/catalog/cart/checkout tests | So sánh stock các màn hình | PLANNED |
| AC-C13-02 | Order concurrency/rollback IT | Đặt vượt số lượng cuối | PARTIALLY COVERED |
| AC-C13-03 | Order allocation projection test | Xem order đang giữ hàng | PLANNED |
| AC-C13-04 | Cancellation integration tests | Hủy và retry | PARTIALLY COVERED |
| AC-C13-05 | SKU/Inventory transaction IT | Tạo SKU có opening stock | PLANNED |
| AC-C13-06 | Adjustment unit + integration tests | Nhập/xuất kho | PLANNED |
| AC-C13-07 | Idempotency integration test | Retry cùng request | PLANNED |
| AC-C13-08 | Mixed concurrency integration test | Sale khi admin chỉnh stock | PLANNED |
| AC-C13-09 | Repository/MVC tests | Filter lịch sử movement | PLANNED |
| AC-C13-10 | Reconciliation test | Chạy report đối soát | PLANNED |

## Acceptance Scenarios

1. Tạo SKU với opening stock 10; xác nhận Inventory = 10 và một
   `OPENING_BALANCE +10`.
2. Admin nhập thêm 5; xác nhận balance = 15 và movement có actor/reference.
3. Admin xuất 2 hàng hỏng; xác nhận balance = 13 và `DAMAGE_OUT -2`.
4. Admin cố decrease 14; xác nhận request fail và không đổi balance/movement.
5. Customer đặt 3; xác nhận balance = 10, `SALE_OUT -3` và order xuất hiện trong
   held/allocation projection khi còn PENDING/CONFIRMED.
6. Payment/status success không trừ stock lần hai.
7. Hủy order; xác nhận balance = 13 và một `ORDER_CANCEL_RETURN +3`; retry không
   tạo thêm side effect.
8. Race order và admin adjustment trên cùng SKU; xác nhận không lost update,
   không stock âm và mọi command commit đều có movement.
9. Retry adjustment bằng cùng idempotency key; xác nhận không double apply;
   đổi payload bằng cùng key trả conflict.
10. Filter movement theo SKU/reason/date và xác nhận pagination ổn định.

Acceptance status: `UAT PENDING`.

## Open Questions

- Existing Product/SKU update/reconcile field `stock` sẽ được loại khỏi update
  contract hay giữ compatibility bằng cách chuyển thành audited physical-count
  adjustment? Khuyến nghị: chỉ SKU create nhận opening stock; stock update đi
  qua Inventory adjustment API riêng.
- Có cần lưu `balance_before`/`balance_after` trong `stock_movement` ngay ở scope
  đầu hay chỉ signed delta + opening baseline? Khuyến nghị: thêm để admin audit
  dễ hơn nếu transaction lấy được hai giá trị một cách nhất quán.
- Held/allocation projection có cần API Inventory riêng trong scope đầu, hay
  admin Order list đã đủ để trả lời order nào đang giữ hàng? Khuyến nghị: bắt
  đầu từ Order query; chỉ thêm aggregate Inventory projection khi UI có nhu cầu.
