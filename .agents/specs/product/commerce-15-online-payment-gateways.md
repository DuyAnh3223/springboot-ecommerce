# SPEC-COMMERCE-15 — MoMo, VNPAY và payOS

## Status

Accepted — user / 2026-09-06 (explicit ACCEPT).

User đã chốt: tích hợp cả ba gateway, sandbox/test trước, bật riêng từng gateway
khi có cấu hình. Các chính sách lifecycle dưới đây đã được accept cùng spec.

## Context / Current Behavior and Evidence

- Commerce-14 và ADR-006 đã accepted: Payment là source of truth, Order 1:N
  Payment, COD và MOCK; initial attempt cùng transaction tạo Order.
- PaymentService xử lý mock result; OrderPaymentService khóa Order trước khi
  áp dụng result/retry. Mock terminal result không thay đổi và late success bị từ chối.
- OrderCreationService trừ stock khi tạo đơn; cancellation hoàn stock/voucher.
  ADR-003/005 tiếp tục áp dụng: payment success không trừ stock lần hai.
- Chưa có gateway HTTP adapter, callback thật, truy vấn đối soát hay expiry job.
- Spec này thay phạm vi mock-only của Commerce-14; giữ nguyên
  ownership và lock ordering của ADR-006. Không tự đánh dấu tài liệu cũ superseded.
- Baseline và giới hạn kiểm chứng được ghi trong plan đi kèm.

## Goal

Khách chọn gateway được bật, mở trang thanh toán và xem kết quả đã được server
xác thực. COD tiếp tục hoạt động. Không dùng mock làm fallback khi gateway lỗi.

## Requirements

### R-C15-01 — Cấu hình và môi trường

- VNPAY và MoMo dùng sandbox/test mặc định; mỗi provider có enabled flag riêng.
- Disabled provider không xuất hiện trong lựa chọn và request trực tiếp bị từ chối
  trước tác động tạo đơn/stock. Enabled nhưng thiếu credentials/URL phải fail fast.
- payOS không có sandbox riêng theo tài liệu chính thức. Implement adapter và
  contract tests offline; mặc định disabled. Live smoke test cần người dùng đồng
  ý riêng vì giao dịch dùng API production và tài khoản ngân hàng thật.
- Secrets lấy từ environment, không commit/log/đưa vào response. Có env example
  không chứa secret, hướng dẫn public HTTPS callback và đăng ký merchant.

### R-C15-02 — Tạo checkout và retry

- Amount/currency lấy từ Order server; chỉ VND nguyên, kiểm tra giới hạn của
  gateway trước tạo đơn. Method COD/ONLINE; provider INTERNAL/MOMO/VNPAY/PAYOS.
- Tạo initial Payment trong transaction tạo Order. Persist merchant attempt ID
  trước gọi gateway; network call ở ngoài transaction và ngoài khóa Order/Redis.
- Retry cùng idempotency key trả cùng attempt. Một order chỉ có một attempt
  chưa xác định kết quả; không đổi gateway hoặc tạo charge mới khi attempt cũ
  còn pending/unknown. Retry khác key chỉ sau kết quả không thu tiền đã xác nhận.
- Timeout/network error không chứng minh thất bại: giữ pending với trạng thái
  initiation UNKNOWN; truy vấn bằng merchant ID trước quyết định gửi lại.
- Crash sau remote create, trước local save phải phục hồi cùng merchant ID;
  không generate ID mới. Response create đến sau callback không ghi đè success.
- Retire public/admin mock endpoints khỏi application; fake transport chỉ ở tests.
  Existing MOCK rows vẫn đọc được, không tự xóa hoặc chuyển thành giao dịch thật.

### R-C15-03 — Xác thực và áp dụng kết quả

- Browser return chỉ dẫn về trang trạng thái, không cập nhật paid từ query string.
- Callback/query response phải được xác thực theo protocol provider, khớp merchant,
  attempt, amount, currency trước áp dụng. Không tin kết quả chưa xác thực.
- Giữ unique(provider, provider_reference) cho transaction ID thật; merchant request
  ID/payment-link ID lưu riêng. Không dùng failure sentinel như transaction ID.
- Callback và reconciliation dùng cùng service, khóa Order rồi Payment; ghi nhận
  Payment, confirm Order và history trong một DB transaction. ACK thành công sau commit.
- Duplicate success là no-op; failure đến sau success không downgrade. Callback trước
  create response vẫn có thể tìm attempt bằng merchant ID đã persist.
- Callback route public giới hạn riêng từng provider; owner/admin authorization
  vẫn áp dụng cho create/read/retry. Provider ACK đúng giao thức, không bọc API envelope.
- payOS webhook verification sample không tạo hoặc thanh toán một Order giả.

### R-C15-04 — Tiền về muộn hoặc thanh toán thừa

- Phân biệt financial status với việc áp dụng tiền vào Order. Khi xác thực thu tiền
  thành công sau local cancellation/expiry, vẫn ghi nhận SUCCEEDED, paidAt và reference.
- Payment có applicationStatus NONE/APPLIED/REVIEW_REQUIRED và reviewReason. Chỉ
  một attempt được APPLIED cho mỗi Order; các khoản thu thêm phải giữ bằng chứng và
  REVIEW_REQUIRED. Không tuyên bố hệ thống đảm bảo gateway không thể thu tiền hai lần.
- Order còn PENDING, chưa quá hạn và chưa có applied payment: success confirm một lần.
- Order đã cancelled/quá hạn hoặc đã có payment khác applied: không reopen, không
  trừ lại stock, không tự hoàn tiền. Hiển thị cần xử lý cho admin và thông báo khách
  đang cần kiểm tra thanh toán; không hiển thị một order đã huỷ như đã confirm.
- Signature đúng nhưng amount/correlation sai: không confirm Order; ghi sự kiện
  kiểm tra có giới hạn dữ liệu, hiển thị admin khi xác định được attempt. Không
  đánh dấu một khoản sai amount là đã thanh toán đủ cho Order.

### R-C15-05 — Hết hạn và đối soát tối thiểu

- Deadline online 15 phút, configurable, tính một lần khi tạo order;
  retry không gia hạn. COD không có payment deadline. Đồng bộ gateway expiry theo
  khả năng provider, không coi expiry local là bằng chứng gateway đã ngừng thu tiền.
- Deadline thuộc Payment checkout policy, truy xuất từ initial attempt; Order
  không lưu payment lifecycle thứ hai.
- Job đối soát pending/unknown và job hết hạn dùng persisted work state, batch giới
  hạn, backoff và khóa để an toàn khi restart/nhiều instance. Truy vấn network ngoài lock.
- Khi quá deadline, nếu chưa applied success, cancel Order và hoàn stock/voucher
  đúng một lần theo lifecycle hiện có. Cùng kiểm tra deadline khi callback đến để
  kết quả không phụ thuộc job có chạy kịp hay không. Local cancellation không ngăn
  nhận success muộn; tiếp tục reconciliation có giới hạn, sau đó hiện admin review.
- Không tự coi timeout là FAILED hoặc bỏ qua việc theo dõi unknown vô thời hạn.
  Retry budget/window là cấu hình có tài liệu; hết budget chuyển review, không giả success/failure.

### R-C15-06 — UI và tương thích COD

- Checkout hiển thị COD và gateway enabled. Sau tạo đơn, mở checkout URL do adapter
  trả về; nếu khởi tạo chưa rõ thì hiện đang kiểm tra, không khuyến khích trả tiền lại.
- Trang return/order detail đọc trạng thái từ backend, polling có giới hạn, cho
  retry khi backend cho phép. Không có secret hoặc raw callback ở customer response.
- Admin xem payment attempts, application status/review reason để nhận biết trường
  hợp cần xử lý; không thêm nút giả lập paid hay refund trong scope này.
- COD delivery/cancel và response payment summary cũ vẫn tương thích. ONLINE là
  giá trị mới phải cập nhật đồng bộ client; mọi online unpaid đều bị chặn manual confirm.

## Acceptance Criteria

| ID | Given / When / Then | Requirements |
|---|---|---|
| AC-C15-01 | Gateway disabled hoặc thiếu cấu hình: không thể tạo checkout; enabled invalid config không khởi động âm thầm. | R-C15-01 |
| AC-C15-02 | Owner tạo checkout với amount client giả: chỉ dùng total server, request ký đúng protocol và dùng stable attempt ID. | R-C15-02 |
| AC-C15-03 | Đồng thời retry cùng/khác key, remote timeout hoặc crash: không phát sinh attempt mới khi kết quả còn unknown; phục hồi qua query. | R-C15-02 |
| AC-C15-04 | Signature/merchant/reference/amount sai: không confirm; browser redirect giả không làm paid; callback hợp lệ hoạt động qua security filters. | R-C15-03, R-C15-04 |
| AC-C15-05 | Callback success lặp/đồng thời/query cạnh tranh: đúng một confirmation/history, không trừ stock thêm; create response hoặc failure muộn không downgrade. | R-C15-03 |
| AC-C15-06 | Success cạnh tranh cancel/expiry: kết quả theo Order lock/deadline; hoàn stock/voucher tối đa một lần; tiền về muộn được lưu và hiện review. | R-C15-04, R-C15-05 |
| AC-C15-07 | Hai giao dịch thực sự thu tiền: chỉ một APPLIED, khoản còn lại được lưu và REVIEW_REQUIRED; duplicate transaction không gán sang order khác. | R-C15-03, R-C15-04 |
| AC-C15-08 | Lost callback/restart: job query phục hồi success; lỗi mạng không đổi thành failed; hết budget được đưa vào review. | R-C15-05 |
| AC-C15-09 | DB rollback khi confirm/cancel: Payment, Order, history và compensation không commit một phần; callback được retry theo protocol. | R-C15-03, R-C15-05 |
| AC-C15-10 | Mỗi adapter có fixture ký độc lập, happy/failure/pending/tampering/query tests; payOS verification sample không mutate order. | R-C15-01, R-C15-03 |
| AC-C15-11 | Khách chọn gateway enabled, return/reload thấy trạng thái server và retry đúng điều kiện; admin thấy late-success review; COD không regression. | R-C15-06 |

## Non-Goals

- Refund API/full-partial refund, settlement ledger, dispute, recurring payment.
- Inventory reservation, thay thời điểm trừ stock, chuyển kiến trúc sang message broker.
- Bật production, tạo thanh toán thật payOS hoặc dùng credentials thật trong test tự động.

## API Contract

- Order create: giữ contract COD; thêm paymentMethod=ONLINE và paymentProvider.
- GET /payments/providers: danh sách gateway enabled, không có secrets.
- POST /orders/{orderCode}/payments/checkout: owner, UUID idempotency key và provider;
  reuse initial attempt hoặc retry hợp lệ. Không nhận amount từ client.
- GET /orders/{orderCode}/payments: owner, trả attempts và checkout/status/retryAllowed
  từ server; không trả raw payload. Admin read hiện có thêm review fields.
- POST /payments/callbacks/momo; GET /payments/callbacks/vnpay;
  POST /payments/callbacks/payos: provider protocol-specific request/ACK.
- Return URL cấu hình server dẫn tới trang order/payment result; không nhận arbitrary
  redirect URL từ client. Response checkout chỉ chứa URL từ provider được phép.
- Errors: provider unavailable, payment pending, retry not allowed, order expired,
  amount unsupported; dùng AppException/ErrorCode theo convention hiện có.

## Data / Architecture

- Giữ ADR-006 Payment ownership và initial attempt atomic với Order. Bổ sung
  merchantRequestId, providerCheckoutId, checkoutUrl, deadline, initiation state,
  applicationStatus/reviewReason, reconciliation scheduling metadata trên Payment.
- Financial terminal failure có thể được sửa thành success chỉ khi bằng chứng thu
  tiền đã xác thực; không áp dụng mock terminal guard cho gateway thật.
- Unique merchant correlation theo provider; unique transaction reference như cũ;
  DB bảo vệ tối đa một APPLIED per order. Khóa Order là lock order chung.
- Lưu callback evidence tối thiểu/phân quyền, không log signatures, secrets hoặc
  bank/customer payload nguyên vẹn; giới hạn kích thước input và retention có tài liệu.
- Schema delta phải review riêng theo cơ chế schema của repo trước khi chạy; không
  reset database hoặc xóa MOCK data ngầm. Không đổi accepted ADR trong trạng thái Draft.

## Verification / Traceability

- Unit: money conversion, signature fixtures, state transitions, retry/deadline (clock injected).
- MVC: real security chain, ownership, anonymous callbacks, forged return, ACK formats.
- Adapter component: local HTTP stub, exact request/response signatures, timeout/query.
- PostgreSQL integration: uniqueness, concurrency, rollback, scheduler restart recovery.
- Client: checkout/return/retry/review interaction tests, typecheck, lint, build.
- AC-C15-01..11: automated implementation evidence is recorded in the Commerce-15 walkthrough and plan. Unit, protocol, MVC and PostgreSQL workflow/race checks pass; live gateway UAT remains pending because merchant credentials and public callbacks are deployment inputs.
- Manual: VNPAY/MoMo sandbox happy/failure/cancel/duplicate/lost callback; COD regression.
  payOS offline trước, live UAT chỉ sau đồng ý riêng. Acceptance: UAT PENDING.

## Acceptance Decision

User ACCEPT 2026-09-06 chốt: online deadline 15 phút configurable,
late/extra success đưa admin review không auto refund, và scope gồm checkout/return UI.
Credentials/registered callback URL là deployment inputs, không cần đưa secret vào chat.

## Official Protocol References (checked 2026-09-06)

- MoMo create: https://developers.momo.vn/v3/vi/docs/payment/api/wallet/onetime/
- MoMo IPN: https://developers.momo.vn/v3/docs/payment/api/result-handling/notification/
- VNPAY payment/IPN: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- VNPAY query: https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html
- payOS create/query/webhook: https://payos.vn/docs/api/
- payOS environment: https://payos.vn/docs/moi-truong-test/
