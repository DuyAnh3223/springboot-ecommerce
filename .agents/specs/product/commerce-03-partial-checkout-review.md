# SPEC-COMMERCE-03 — Partial Checkout Review Contract

## Status

Accepted

Accepted by / date: Product & Architecture Team / 2026-08-17

## Context

`POST /orders/checkout-review` phải preview đúng tập cart items người dùng chọn,
chỉ dùng dữ liệu server và trả một reviewed snapshot tối thiểu để Plan 04
semantic-compare trước khi tạo order. Bất kỳ thay đổi nào ảnh hưởng order giữa
review và create đều yêu cầu customer review lại (xem
`.agents/decisions/ADR-002-checkout-reviewed-snapshot-contract.md`).

Hiện tại `checkoutReview` review toàn bộ cart, trả exception 400 cho business
issue, response thiếu dữ liệu order-affecting (eligible subtotal, voucher
applicability, `canPlaceOrder`), và wire contract chưa khóa cho Plan 04.

## Current Behavior and Evidence

- Relevant flow: `OrderController.checkoutReview` → `OrderService.checkoutReview`
  → `CartRepository.findByUserIdAndStatus` → toàn bộ `cart.items` →
  `VoucherValidator.validateForCheckout` / `VoucherService.calculateEligibleSubtotal`
  / `getDiscount`.
- Existing contract/spec: SPEC-COMMERCE-02 (accepted), ADR-002 (accepted),
  PLAN-COMMERCE-03.
- Baseline command: `./mvnw -Dtest="OrderServiceTest,*OrderController*,*Checkout*" test`
  từ `server/`.
- Baseline result: NOT RUN.

## Goal

- API chỉ nhận selected SKU IDs và optional voucher code.
- Preview chỉ dùng active cart của current user; selection được owner-validate,
  sort và deduplicate.
- Item không bán được hoặc thiếu stock vẫn xuất hiện trong response với typed
  issue code và `canPlaceOrder=false` (HTTP 200, không exception 500).
- Mọi amount, price, stock, sellability lấy từ database, không tin client.
- Response là reviewed snapshot tối thiểu, ổn định, không có fingerprint/token,
  đủ dữ liệu order-affecting cho Plan 04 semantic comparison.
- Client-controlled voucher validation không còn là authoritative path.

## Requirements

### R-C03-01: Selected-Item Request and Ownership

- Request gồm `selectedSkuIds` (bắt buộc, không null/rỗng, mỗi ID dương) và
  optional `voucherCode`.
- Service normalize: deduplicate và sort tăng dần các ID.
- Mọi selected SKU ID phải thuộc active cart của current user; nếu không,
  trả owner-safe error (không tiết lộ cart của user khác).
- Không nhận quantity, price, subtotal, shipping, userId hoặc eligible subtotal
  từ client.

### R-C03-02: Server-Owned Review Amounts and Typed Issues

- `lineTotal = databaseUnitPrice * cartQuantity`, `subtotal` là tổng selected
  line totals.
- Sellability đánh giá theo thứ tự deterministic:
  1. Cart quantity null/không dương.
  2. SKU không tồn tại.
  3. SKU inactive.
  4. Product unpublished/draft.
  5. Stock nhỏ hơn quantity.
- Issue code machine-readable, ổn định, không chứa raw exception; tái sử dụng
  `ErrorCode` khi ngữ nghĩa khớp.
- Expected business issue là review result HTTP 200 với issue code và
  `canPlaceOrder=false`, không phải exception 500.

### R-C03-03: Minimal Reviewed Snapshot

- Response chứa đủ dữ liệu order-affecting: items sort ổn định theo SKU ID
  (SKU ID, cart quantity, authoritative unit price, line total), voucher
  identity/applicability, eligible subtotal, discount, subtotal, shipping fee,
  total amount, `canPlaceOrder`.
- Không trả fingerprint, signed token, issued-at hoặc expiry.
- Display-only fields (product name, SKU code, image, available stock) có thể
  có nhưng không bắt buộc trong expectation snapshot Plan 04.

### R-C03-04: Stable API Contract

- Wire contract (request/response JSON) là contract của Plan 04 trở đi.
- `totalAmount = max(0, subtotal + shippingFee - discountAmount)`.
- Voucher invalid là expected review result HTTP 200: `applicable=false`,
  `discountAmount=0`, `canPlaceOrder=false`.
- Shipping fee v1 đọc từ property backend, chỉ cộng khi selection hợp lệ
  không rỗng; item invalid thì `canPlaceOrder=false` và không có order được tạo.
- Validation DTO sai vẫn trả HTTP 400; auth lỗi theo Plan 01.
- Preview không reserve price, stock hoặc voucher; không expiry/countdown.

### R-C03-05: Legacy Client-Controlled Voucher Validation Containment

- Audit consumer của `POST /vouchers/validate` /
  `VoucherDiscountRequest` / client `validateVoucher`.
- Nếu không còn consumer sau contract mới, xóa endpoint, request/response DTO
  và client service export liên quan.
- Nếu còn consumer ngoài commerce không thể chuyển, giữ deprecated adapter
  nhưng adapter phải tự load server context; không tin client total. Ghi blocker
  cụ thể trong walkthrough.

## Acceptance Criteria

### AC-C03-01

Given một request hợp lệ gồm `selectedSkuIds` và optional `voucherCode`,
When `POST /orders/checkout-review` được gọi,
Then API chỉ nhận selected IDs và optional voucher code; các field khác
(quantity, price, subtotal, shipping, userId) không được chấp nhận.

### AC-C03-02

Given user chọn SKU IDs thuộc active cart của chính mình,
When service xử lý,
Then selection được owner-validate, sort tăng dần và deduplicate.

### AC-C03-03

Given cart có 3 items và user chọn 2 items,
When checkout review được gọi,
Then unselected item không ảnh hưởng subtotal, voucher, shipping hoặc reviewed
snapshot.

### AC-C03-04

Given cart quantity và giá SKU trên database,
When review được gọi,
Then price, stock, sellability lấy từ database hiện tại, không dùng stale
price từ request hoặc cart.

### AC-C03-05

Given item không bán được hoặc thiếu stock,
When review được gọi,
Then response trả typed issue (HTTP 200), item vẫn xuất hiện trong response,
`canPlaceOrder=false`, không exception 500.

### AC-C03-06

Given review thành công hoặc có business issue,
Then response cung cấp reviewed snapshot tối thiểu, ổn định, đủ dữ liệu
order-affecting cho Plan 04 semantic comparison và không chứa fingerprint/token/
expiry.

### AC-C03-07

Given legacy endpoint `POST /vouchers/validate` và client `validateVoucher`,
When consumer audit hoàn tất,
Then endpoint bị loại bỏ hoặc blocker được ghi rõ trong walkthrough.

### AC-C03-08

Given implementation hoàn tất,
Then checkout docs/tests được cập nhật theo contract mới.

### AC-C03-09

Given toàn bộ work hoàn tất,
Then không stage/commit/push.

## Non-Goals

- Tạo order và Idempotency-Key.
- Xóa selected items khỏi cart.
- Inventory/voucher mutation.
- Checkout frontend.
- Semantic comparison snapshot (Plan 04).

## Edge Cases

- `selectedSkuIds` null/rỗng hoặc chứa ID không dương → HTTP 400.
- SKU ID thuộc cart user khác hoặc không thuộc cart → owner-safe error, không
  lộ thông tin cart.
- SKU inactive, product unpublished/draft, stock < quantity → typed issue,
  `canPlaceOrder=false`.
- Voucher invalid/expired/min-order/scope → HTTP 200, `applicable=false`,
  discount 0, `canPlaceOrder=false`.
- Voucher code blank → không áp voucher.
- Duplicate/unsorted IDs → normalize thành sorted unique list.
- Item không chọn → không tham gia bất kỳ amount hoặc snapshot nào.

## Domain Invariants

- Database là nguồn sự thật duy nhất cho mọi amount; client snapshot chỉ là
  untrusted input cho semantic comparison ở Plan 04.
- Không map monetary field từ request trực tiếp vào `Order`/`OrderItem`.
- `totalAmount >= 0`; `discountAmount <= eligibleSubtotal`; line total =
  database unit price × cart quantity.
- Preview không giữ price, stock hoặc voucher; không expiry/countdown.

## API Contract

### Request

```http
POST /orders/checkout-review
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "selectedSkuIds": [42, 17, 42],
  "voucherCode": " summer "
}
```

- `selectedSkuIds`: bắt buộc, không null/rỗng, mỗi ID dương. Service deduplicate
  và sort tăng dần → `[17, 42]`.
- `voucherCode`: nullable/blank; blank = không áp voucher; normalize
  `trim().toUpperCase(Locale.ROOT)`.

### Response

Giữ envelope `ApiResult<CheckoutResponse>` hiện có:

```json
{
  "items": [
    {
      "skuId": 17,
      "skuCode": "SKU-17",
      "productName": "Tên sản phẩm",
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
  "voucher": {
    "code": "SUMMER",
    "applicable": true,
    "issueCode": null
  },
  "canPlaceOrder": true
}
```

Nested DTO tên có thể theo convention repo hiện có, nhưng JSON wire fields trên
là contract của Plan 04.

### Errors

- `400` — `selectedSkuIds` null/rỗng, ID không dương, cart rỗng (nếu có), SKU
  không thuộc active cart (owner-safe), malformed JSON.
- `401` — unauthenticated.
- Expected business issues (unsellable, insufficient stock, invalid voucher)
  là HTTP 200 với typed issue và `canPlaceOrder=false`.

## Security / Authorization

- Endpoint bắt buộc authenticated user; user resolve qua `AuthService`.
- Selection luôn được kiểm tra thuộc active cart của current user; không expose
  cart/thông tin của user khác.
- Không nhận hoặc tin `userId`, price, subtotal từ client.

## Data / Persistence Considerations

- Preview read-only (`@Transactional(readOnly = true)`), không persist synced
  price, không reserve stock/voucher, không tăng `usedCount`.
- SKU/product được re-fetch từ database qua repository (tránh stale object).

## Verification Strategy

### Unit

- `OrderServiceTest`: normalization, ownership, subset selection, sellability
  issues, price từ database, voucher invalid review, snapshot fields, absence
  fingerprint/token.

### MVC / Component

- `OrderControllerTest` (`@WebMvcTest`): auth required, request validation,
  response fields/amount serialization, absence fingerprint/token, business
  issue HTTP 200.

### Integration

- Không cần integration test mới: preview không mutate DB; logic đã được unit
  và MVC slice phủ.

### Static / Build / Runtime

- `./mvnw -Dtest="OrderServiceTest,*OrderController*,*Checkout*" test`
- `./mvnw test`

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
|---|---|---|---|
| AC-C03-01 | `OrderControllerTest` request validation | UAT 1 | PLANNED |
| AC-C03-02 | `OrderServiceTest` normalize/owner tests | UAT 1 | PLANNED |
| AC-C03-03 | `OrderServiceTest` subset selection | UAT 2 | PLANNED |
| AC-C03-04 | `OrderServiceTest` DB price/stock | UAT 1 | PLANNED |
| AC-C03-05 | `OrderServiceTest` + `OrderControllerTest` typed issues | UAT 3 | PLANNED |
| AC-C03-06 | `OrderServiceTest` + `OrderControllerTest` snapshot | UAT 4 | PLANNED |
| AC-C03-07 | consumer audit | — | PLANNED |
| AC-C03-08 | docs/tests updated | — | PLANNED |
| AC-C03-09 | git status | — | PLANNED |

## Acceptance Scenarios

1. Review cùng tập SKU chọn với thứ tự input khác nhau → cùng normalized items
   và amounts.
2. Review subset của cart lớn hơn → unselected items không ảnh hưởng money
   hoặc snapshot.
3. Review invalid voucher và unsellable/insufficient-stock → typed HTTP 200
   review với `canPlaceOrder=false`.
4. Response không chứa fingerprint/token/expiry và chứa đủ field order-affecting
   cho Plan 04.

## Open Questions

- Không còn material question: ADR-002 và PLAN-COMMERCE-03 đã khóa contract;
  shipping fee 30.000₫ chuyển thành property backend (`app.checkout.shipping-fee`).
