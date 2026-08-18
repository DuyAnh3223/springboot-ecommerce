# ADR-002 — Checkout Reviewed Snapshot Contract

## Status

Accepted

## Context

Checkout review và create order là hai HTTP request riêng. Giữa hai request,
giá, cart quantity, sellability, stock sufficiency, voucher, shipping fee hoặc
amount có thể thay đổi. Create order phải phát hiện mọi thay đổi ảnh hưởng đến
order và yêu cầu customer review lại, nhưng không được tin dữ liệu tiền do
client gửi để tạo order.

Các phương án fingerprint và signed review token làm request ngắn hơn hoặc
chống client sửa baseline, nhưng không thay thế authoritative recomputation,
locking, atomic stock decrement, voucher guard hay idempotency. Threat model v1
không coi việc client tự bỏ qua lớp bảo vệ review của chính họ là một rủi ro tài
chính, miễn là server luôn ghi order bằng dữ liệu authoritative.

## Decision

- `POST /orders/checkout-review` trả một checkout snapshot do server tính từ
  active cart, selected items, current catalog state, voucher và shipping.
- Frontend hiển thị và gửi lại một expectation snapshot tối thiểu khi gọi
  `POST /orders`. Không dùng review fingerprint hoặc signed review token.
- Expectation snapshot chỉ chứa field ảnh hưởng order: SKU ID, quantity, unit
  price, line total, voucher identity/applicability, eligible subtotal,
  subtotal, discount, shipping fee và total.
- Create order coi toàn bộ expectation snapshot là untrusted input chỉ dùng để
  semantic-compare. Order, order items, discount, shipping và total luôn được
  tạo từ checkout recomputed bằng dữ liệu authoritative trong transaction.
- Sau khi acquire các distributed locks cần thiết, create order reload state,
  recompute checkout và semantic-compare với snapshot đã review. Bất kỳ thay
  đổi ảnh hưởng order nào, kể cả price tăng hoặc giảm, đều trả
  `409 CHECKOUT_CHANGED` với latest checkout và không commit mutation.
- So sánh stock theo khả năng đáp ứng requested quantity, không so raw stock
  count khi stock vẫn đủ. Tương tự, thay đổi voucher counter không gây mismatch
  nếu applicability và monetary outcome không đổi.
- Checkout review không giữ giá, stock hoặc voucher và không có expiry/countdown.
  Stock/voucher chỉ được mutate atomically khi create order thành công.
- `Idempotency-Key` là cơ chế riêng để chống retry/double-submit; reviewed
  snapshot không thay thế idempotency.

## Rationale

Contract này giữ invariant user-visible đơn giản: order chỉ được tạo khi dữ
liệu ảnh hưởng order giống lần review gần nhất. Semantic comparison dễ đọc,
debug và trả chi tiết thay đổi hơn fingerprint. Nó cũng tránh signing key,
canonical hash payload, expiry, rotation và token-version lifecycle trong khi
vẫn giữ database là nguồn sự thật duy nhất cho order.

## Alternatives Considered

### Deterministic fingerprint

Frontend chỉ gửi SHA-256 của canonical checkout. Payload nhỏ hơn nhưng backend
vẫn phải recompute toàn bộ checkout, duy trì canonicalization/versioning và
semantic-compare thêm nếu cần giải thích field đã đổi. Không chọn vì lợi ích
network không đáng kể với checkout thông thường.

### Signed review token

Token mang snapshot và chống client sửa baseline mà không cần server-side
storage. Không chọn cho v1 vì không reserve tài nguyên, không thay thế database
revalidation và thêm key management, expiry, rotation, payload/versioning cùng
UX refresh không cần thiết cho threat model hiện tại.

### Server-side checkout session

Server lưu snapshot và client chỉ gửi session ID. Phù hợp nếu tương lai cần
reserve price/stock/voucher hoặc revoke checkout, nhưng thêm persistence, TTL,
cleanup và concurrency cho một preview hiện được yêu cầu read-only.

### Always accept current authoritative checkout

Đơn giản nhất nhưng có thể tạo order với giá hoặc total khác dữ liệu customer
vừa xác nhận. Không đáp ứng contract review-before-create.

## Consequences

### Positive

- Không có fingerprint/token, signing secret, expiry hoặc checkout-session
  storage.
- Mismatch có thể chỉ rõ price, quantity, voucher, shipping hay total đã đổi.
- Order vẫn chống client-controlled money vì snapshot không bao giờ là nguồn
  dữ liệu persistence.
- Không giữ stock khi customer chỉ mở checkout.

### Negative

- Create-order request lớn hơn một fingerprint và lặp lại một phần response
  review.
- Client có thể sửa expectation để tự bỏ qua bước review; server vẫn phải bảo
  đảm việc này không thay đổi authoritative order amounts hoặc inventory.
- Mọi price change, kể cả giảm, yêu cầu một lần review và submit mới.
- Frontend phải giữ snapshot và xử lý `409 CHECKOUT_CHANGED` bằng latest review.

## Constraints for Future Changes

- Không thêm fingerprint/token song song với snapshot nếu chưa có requirement
  mới và ADR cập nhật hoặc supersede quyết định này.
- Không map monetary field từ request trực tiếp sang `Order`/`OrderItem`.
- Recompute, compare và mutation phải nằm trong lock/transaction boundary đủ
  để không có khoảng trống check-then-act.
- Atomic stock/voucher guards và idempotency vẫn bắt buộc dù snapshot match.
- Nếu cần price/stock hold, thiết kế một server-side checkout session/reservation
  có TTL riêng và supersede ADR này; không biến reviewed snapshot thành reservation.

## Supersedes

None.

## Superseded By

None.
