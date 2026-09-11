# SPEC-COMMERCE-14 — Payment L1: COD và mock payment

## Status

Accepted

Accepted by / date: user / 2026-09-05.

Online extension: Commerce-15 accepted by user on 2026-09-06 supersedes the
mock-only runtime scope. COD and Payment ownership remain valid; real late
success follows Commerce-15 financial-recording/review policy. Mock HTTP
controllers are retained only as test fixtures.

Implementation status: code and automated Payment/Order verification complete;
manual acceptance remains `UAT PENDING`.

## Context

Payment cần sở hữu trạng thái thanh toán, thay cho trạng thái lưu trong Order.
L1 hỗ trợ COD và mock trả trước, nhiều attempts và basic idempotency kể cả
concurrency. Gateway thật thuộc L2; refund đầy đủ thuộc L3.

## Pre-implementation Behavior and Evidence

- `OrderCreationService.createOrder` dùng reviewed checkout, idempotency key,
  Redis locks và `TransactionTemplate`; tạo COD Order cùng stock/voucher/cart.
- `OrderLifecycleService` khóa Order khi huỷ/update; delivery ghi `Order.PAID`,
  cancellation ghi `Order.CANCELLED` ở payment status.
- `OrderMapper` map payment fields từ Order sang response hiện tại.
- `modules/payment/entity/Payment.java` hiện là file rỗng, chưa có module chạy.
- Commerce-04 R-C04-06 và Commerce-05 R-C05-01 đang là contract COD hiện hành.
- ADR-003/ADR-005 giữ nguyên: tạo Order trừ stock, huỷ hoàn theo OrderItem;
  payment success/delivery không trừ stock lần hai.
- Baseline command/result xem plan liên kết; source audit không chứng minh runtime.

## Goal

Payment là nguồn dữ liệu thanh toán duy nhất; Order giữ payable amount và
fulfillment lifecycle. Giữ hành vi COD và response hiện tại, thêm mock có giới
hạn môi trường để kiểm thử success/failure/retry và race conditions.

## Requirements

### R-C14-01 — Ownership và data model

- Mỗi Payment là một attempt, Order 1:N Payment; không unique riêng order_id.
- Fields: id, order_id, provider, method, status, amount, currency,
  provider_reference, raw_payload, paid_at, created_at.
- Thêm `idempotency_key` cho attempt creation, unique `(order_id, idempotency_key)`.
  Initial attempt dùng key do server cấp ổn định; retry nhận UUID key từ request.
- Payment status: PENDING, SUCCEEDED, FAILED, CANCELLED. Method/provider L1:
  COD/INTERNAL và MOCK/MOCK. Provider reference có thể null trước kết quả và với COD.
- FK Order; amount/currency/method/provider/status/created_at/key không null;
  amount không âm, VND là currency L1. paid_at chỉ có khi SUCCEEDED.
- Unique `(provider, provider_reference)`; reference không rỗng nếu được gửi.
  raw_payload là dữ liệu audit đã lọc, không là input quyết định nghiệp vụ và
  không xuất qua customer/admin payment read API.
- Bỏ persisted Order.paymentStatus và Order.paymentMethod; đọc từ Payment vì
  đọc từ Payment vì L1 không có use case riêng cho lựa chọn phương thức ban đầu.
  Không đổi phương thức giữa các attempts của cùng Order trong L1.
- Không lưu thêm gatewayTransactionId song song provider_reference cùng nghĩa.

### R-C14-02 — Create order và initial payment

- Tạo initial PENDING Payment cùng transaction Order/items/stock/voucher/cart.
  Payment.amount/currency lấy từ Order total/currency phía server.
- Replay create Order trả Order cũ, không tạo thêm Payment.
- Public checkout UI giữ COD. API create Order hiện tại nhận MOCK chỉ
  khi profile dev/test và `app.payment.mock-enabled=true`; mặc định false.
  Guard cả service, không chỉ controller, và thực hiện trước side effects.
- Mock Order vẫn qua reviewed checkout, auth, address, idempotency và inventory
  guards hiện hành; không tạo một đường insert Order bỏ qua business rules.

### R-C14-03 — COD lifecycle

- COD xác nhận/giao theo Commerce-05. DELIVERED ghi initial Payment SUCCEEDED
  và paid_at, cùng transaction với Order/history; đây là quy ước thu COD của L1.
- Huỷ hợp lệ đóng pending payment thành CANCELLED, hoàn stock/voucher đúng
  một lần trong transaction hiện hữu. FAILED attempts giữ nguyên lịch sử.
- Lặp delivery/cancel trả trạng thái hiện tại, không thêm payment/movement/history.
- COD chỉ có một initial payment; không cung cấp retry thu COD ở L1.

### R-C14-04 — Mock attempts và transition

- Initial mock attempt PENDING; failure chuyển FAILED, Order vẫn PENDING.
- Retry chỉ cho MOCK Order PENDING, chưa có SUCCEEDED và không có PENDING attempt.
  Tạo attempt mới; cùng key replay attempt cũ kể cả attempt đã terminal.
- Không cho tạo attempt sau cancellation/success. Tối đa một pending attempt
  theo service transaction trên cùng Order; request khác key khi còn pending → 409.
- Success hợp lệ chuyển Payment SUCCEEDED, paid_at và Order PENDING → CONFIRMED,
  ghi history một lần; không gọi Inventory để trừ stock.
- Admin không được confirm MOCK chưa trả tiền; success là đường confirm MOCK.
- Chặn cancel MOCK đã SUCCEEDED (kể cả admin) vì L1 chưa có refund.
  Sau success, admin vẫn SHIPPING → DELIVERED theo lifecycle; delivery không
  sửa paid_at hoặc ghi nhận trả tiền lần nữa.
- `allowedTransitions` phải phản ánh cùng guard với command API.
- Không tự huỷ đơn khi attempt failed; không có timeout/auto-expiry L1.

### R-C14-05 — Kết quả và idempotency

- Mock result chứa paymentId, providerReference, outcome SUCCEEDED/FAILED,
  amount, currency; server cố định provider MOCK.
- Kiểm tra quyền/môi trường, Order/Payment binding, amount/currency/reference
  trước khi xử lý replay. Amount so sánh theo giá trị số tiền, không theo scale.
- Exact duplicate cùng kết quả/reference/amount/currency → 200, không side effects.
- Terminal nhận kết quả khác → 409, giữ dữ liệu hiện tại; FAILED không hồi sinh.
- Reference đã thuộc attempt khác → 409, không reassociate giao dịch.
- Mock success sau Order cancelled → 409, không reopen/ghi paid_at/đụng stock.
- `if SUCCEEDED return` và unique reference không đủ bảo vệ concurrency:
  tất cả mutation cùng serialize theo Order row, rồi đọc lại trạng thái Payment.
- Không áp dụng policy bỏ qua late success này cho gateway thật L2: khi có tiền
  thật phải lưu sự thật provider và có đường xử lý ngoại lệ/hoàn tiền riêng.

### R-C14-06 — Atomicity và phối hợp module

- Cùng database transaction cho Payment + Order/history và stock/voucher nếu huỷ.
  Không dùng REQUIRES_NEW, không outbox/message broker trong L1.
- Lock Order trước Payment; cancellation/success/delivery/retry dùng cùng thứ tự.
  Order creation giữ existing Redis lock ordering; không thêm luồng callback lấy
  Redis lock sau Order lock gây đảo thứ tự.
- Payment service sở hữu payment transition; Order service sở hữu fulfillment,
  history và compensation. Không tạo dependency cycle giữa hai service.
- Nếu unique violation xảy ra khi flush, xử lý conflict/reload ngoài transaction
  đã rollback; không catch rồi tiếp tục write trong transaction bị đánh dấu lỗi.

### R-C14-07 — Read contract và compatibility

- Owner đọc payment summary và attempts của đơn mình; admin đọc mọi Order.
- Summary trả method, paymentStatus; attempts trả id, provider, method, status,
  amount, currency, providerReference, paidAt, createdAt theo createdAt/id ổn định.
- Order responses giữ `paymentMethod`, `paymentStatus` và tên enum COD hiện hành.
  Mapping: có SUCCEEDED → PAID; Order cancelled chưa trả → CANCELLED;
  các trường hợp chưa trả khác → UNPAID. FAILED chi tiết nằm trong attempts.
- Đọc Payment theo batch Order IDs cho list; không thêm query payment trên mỗi row.
- Order mới bắt buộc có Payment; missing payment là lỗi integrity, không tự dựng
  record hoặc mặc định PAID/UNPAID che giấu dữ liệu thiếu.

### R-C14-08 — Authorization và mock isolation

- Dùng AuthService và convention errors hiện tại; owner mismatch trả 404.
- Mock result chỉ ADMIN, trong dev/test có flag bật. Đây là simulation endpoint,
  không phải public webhook có thể triển khai để nhận tiền thật.
- Khi flag tắt hoặc ngoài dev/test, mock endpoints không được đăng ký; create
  Order MOCK bị từ chối 400 ở business boundary, kể cả gọi service trực tiếp.
- Profile production luôn tắt mock, kể cả vô tình kích hoạt đồng thời dev/test.
- Không mở security matcher public cho mock; không trả raw_payload hoặc secrets.

## Acceptance Criteria

| ID | Given / When / Then | Requirement |
|---|---|---|
| AC-C14-01 | Tạo/replay COD Order → đúng một initial Payment; failure khi lưu payment rollback Order/stock/voucher/cart | 01,02,06 |
| AC-C14-02 | COD delivery/cancel hợp lệ và lặp → đúng payment status, paid_at, movement/history count | 03,06 |
| AC-C14-03 | Mock fail → Order PENDING; retry khác key → attempt mới; cùng key replay; còn pending → conflict | 02,04,05 |
| AC-C14-04 | Mock success → CONFIRMED/PAID một lần; stock không đổi; delivery giữ paid_at | 04,05,06 |
| AC-C14-05 | Callback trùng đồng thời → một transition/history; cùng reference ở hai attempts → một binding | 01,05,06 |
| AC-C14-06 | Sai amount/currency, missing Order/Payment, terminal conflict → lỗi, không thay dữ liệu | 05,08 |
| AC-C14-07 | Cancel thắng race → success conflict; success thắng → cancel conflict; không có paid Order bị hoàn kho | 04,05,06 |
| AC-C14-08 | Admin confirm MOCK unpaid hoặc cancel MOCK paid → 409; allowedTransitions không gợi ý thao tác đó | 04,07 |
| AC-C14-09 | Owner/admin read → dữ liệu từ Payment, COD JSON tương thích; list đọc theo batch | 07,08 |
| AC-C14-10 | Unauthenticated/other owner/non-admin mock result → bị từ chối; mock off/prod → endpoint không tồn tại | 02,08 |
| AC-C14-11 | Hai transaction tạo retry khác key → tối đa một pending; success attempt cũ đã FAILED → conflict | 04,05,06 |
| AC-C14-12 | DB FK/unique/null constraints đúng; hai reference null hợp lệ; schema không còn Order payment state | 01,06 |

## Non-Goals

Gateway thật, signature gateway, refund/return, partial payment, đổi phương thức,
payment expiry, reservation, reconciliation job, outbox/saga, notification,
payment dashboard hoặc nút mock-pay trên storefront. Không đổi amount calculation.

## Edge Cases / Domain Invariants

AC-C14-01..12 là danh sách acceptance tối thiểu. At most one SUCCEEDED được áp
dụng cho Order trong L1; không được suy rộng thành bảo đảm không double-charge
ở gateway thật. Order payable là nguồn amount; Payment là nguồn trạng thái tiền;
Inventory.onHand là nguồn stock. Không suy ra paid từ Order.CONFIRMED.

## API Contract

Paths bên dưới tương đối với context-path `/abtechzone`; dùng response envelope
hiện tại của dự án, không tạo error envelope mới.

| Method/path | Quyền | Request | Kết quả |
|---|---|---|---|
| POST /orders | Owner, existing API | Existing body; paymentMethod COD hoặc MOCK có guard | Existing Order response; initial attempt atomically |
| GET /orders/{orderCode}/payments | Owner hoặc admin | Không body | 200 summary + attempts, không raw_payload |
| POST /orders/{orderCode}/payments/mock-attempts | Owner, mock enabled | Idempotency-Key UUID; không nhận amount | 200 attempt mới hoặc replay |
| POST /payments/{paymentId}/mock-result | Admin, mock enabled | providerReference, outcome, amount, currency | 200 attempt + summary, kể cả exact replay |

Errors: 400 validation/amount/currency mismatch/mock method disabled;
401 unauthenticated; 403 wrong role; 404 unknown/not-owned Order/Payment hoặc
mock route không đăng ký; 409 lifecycle/reference/active-attempt conflict.
ErrorCode cụ thể được thêm theo convention repository, không hardcode response.

## Data / Persistence Considerations

JPA constraints cho FK/unique/index thông thường; integration dùng PostgreSQL.
Cutover đã được accept trên database dev disposable/schema test sạch; không dùng
`ddl-auto:update` để suy ra cột cũ đã bị drop. Không backfill, dual-write hoặc suy
đoán `paid_at` lịch sử. Không reset một database đang dùng nếu chưa xác định đúng target.

## Verification Strategy / Traceability

| Acceptance | Automated evidence dự kiến | Manual UAT | Status |
|---|---|---|---|
| 01,02,07 | OrderIT + PaymentWorkflowIT: transaction/compensation/race | COD và cancel/success | AUTOMATED PASS; UAT PENDING |
| 03,04,06,08 | PaymentServiceTest + lifecycle/policy unit | Mock fail/retry/success | AUTOMATED PASS; UAT PENDING |
| 05,11,12 | PaymentPersistenceIT + PaymentWorkflowIT: constraints/concurrency | Repeated requests | AUTOMATED PASS; UAT PENDING |
| 09,10 | PaymentControllerTest + Order mapper/query tests + mock config tests | Owner/admin/mock-off | AUTOMATED PASS; UAT PENDING |

Unit cho rules; MockMvc cho HTTP/security; PostgreSQL IT cho lock, constraint,
rollback. Static/build và existing client harness theo plan. Không nhân bản toàn
bộ business matrix ở mọi test level.

## Acceptance Scenarios

Thực hiện UAT-01..07 tại plan liên kết; acceptance hiện là UAT PENDING.

## Accepted Decisions

- MOCK đi qua create Order API chỉ tại dev/test có `app.payment.mock-enabled=true`;
  không thêm storefront mock UI.
- L1 chặn admin confirm MOCK chưa trả và chặn huỷ MOCK đã trả, vì chưa có refund.
- Payment sở hữu persisted method/status; Order không giữ các cột này.
- Cutover dùng schema database dev sạch; không backfill, dual-write hoặc suy đoán
  `paid_at` lịch sử. Không reset một database đang dùng mà chưa xác định đúng target.

## Related Plan / Existing Contracts

- [Payment L1 implementation plan](../../docs/plans/2026-09-05-commerce-14-payment-l1-plan.md)
- [Commerce-04](commerce-04-atomic-idempotent-order-creation.md): R-C04-06 và
  schema/payment mapping đã đồng bộ.
- [Commerce-05](commerce-05-order-lifecycle-cancellation.md): R-C05-01,
  cancellation và allowedTransitions cho MOCK đã đồng bộ; giữ COD.
- [ADR-003](../../decisions/ADR-003-order-items-as-committed-inventory-source.md)
- [ADR-005](../../decisions/ADR-005-inventory-owns-on-hand-stock.md)
