# SPEC-COMMERCE-02 — Authoritative Voucher Checkout Calculator

## Status

Accepted

Accepted by / date: Product & Architecture Team / 2026-08-16

## Context

Hiện tại trong hệ thống thương mại điện tử ABTechZone:
1. Logic tính toán giảm giá voucher (`VoucherService.getDiscount`) đang tính trực tiếp trên toàn bộ `totalOrder` (toàn bộ subtotal của giỏ hàng).
2. Khi áp dụng voucher có phạm vi áp dụng cụ thể (`applyScope = SPECIFIC`), voucher bị áp dụng giảm giá trên toàn bộ giỏ hàng thay vì chỉ tính trên các SKU hợp lệ.
3. `OrderService.checkoutReview` (xem trước đơn hàng) và `OrderService.doCreateOrder` (tạo đơn hàng) có một số điểm sai lệch trong quy trình kiểm tra điều kiện voucher (ví dụ: `maxPerUser` chỉ được kiểm tra khi create order mà không kiểm tra ở preview).
4. `POST /vouchers/validate` (`VoucherDiscountRequest`) nhận `totalOrder` và `userId` do client gửi lên mà không đảm bảo tính toàn vẹn (server-authoritative).
5. Tài liệu backend còn mô tả các scope cũ (`GLOBAL/CATEGORY/PRODUCT`) trong khi mã nguồn thực tế sử dụng `ALL/SPECIFIC`.

## Goal

Thiết lập một bộ tính toán & kiểm tra voucher duy nhất (`VoucherCheckoutCalculator` / `VoucherEvaluator`), mang tính server-authoritative, được dùng chung cho cả checkout preview và order creation. Đảm bảo:
- Subtotal toàn giỏ hàng được dùng để xét ngưỡng đơn hàng tối thiểu (`minOrderValue`).
- Subtotal các mặt hàng hợp lệ (`eligibleSubtotal`) được dùng để tính số tiền giảm giá thực tế theo phạm vi (`ALL` vs `SPECIFIC`).
- Preview và Create Order sử dụng chung 1 logic đánh giá (bao gồm cả giới hạn lượt dùng theo user `maxPerUser`).
- Mọi tính toán số tiền sử dụng `BigDecimal` với scale 2, làm tròn `HALF_UP`, đảm bảo không âm và không vượt quá `eligibleSubtotal`.

## Requirements

### R-C02-01: Authoritative Evaluator
Hệ thống phải có một evaluator/calculator duy nhất cho voucher checkout, không có side effect, tính toán độc lập và thuần túy (pure calculation/validation).

### R-C02-02: Full / Eligible Subtotal Semantics
- `fullSubtotal`: Tổng giá trị tiền hàng của các mặt hàng được chọn trong giỏ hàng (tính bằng server-owned `unitPrice * quantity`).
- `minOrderValue`: Được kiểm tra dựa trên `fullSubtotal` (trước phí vận chuyển, trước giảm giá). Phí vận chuyển không tham gia vào ngưỡng này.
- `eligibleSubtotal`:
  - Khi `applyScope == ALL`: Bằng `fullSubtotal`.
  - Khi `applyScope == SPECIFIC`: Tổng giá trị tiền hàng của các mặt hàng trong giỏ hàng có `ProductSku` thuộc danh sách `voucher.productSkus`.
  - Khi `applyScope == SPECIFIC` nhưng không có mặt hàng nào khớp: `eligibleSubtotal = 0`, voucher không hợp lệ/không áp dụng được (`applicable = false`, discount = 0).

### R-C02-03: Server-Owned Context & Preview/Create Parity
- Mọi context đầu vào (`VoucherEvaluationContext`) đều do server khởi tạo từ dữ liệu xác thực:
  - User ID / User entity đã xác thực từ security context.
  - Voucher entity hoặc voucher code được load từ cơ sở dữ liệu.
  - Danh sách các dòng giỏ hàng (SKU ID, quantity, unit price từ DB).
  - Số lượt đã dùng của user (`userUsageCount`) được load từ DB.
- Cả `checkoutReview` và `doCreateOrder` đều truyền cùng format context và nhận cùng kết quả đánh giá từ evaluator.
- Khi tạo đơn hàng (`createOrder`), server tái đánh giá lại voucher dưới transaction / distributed lock, không tin tưởng số tiền client gửi.

### R-C02-04: Deterministic Issues and Money Invariants
- Chuẩn hóa mã voucher: `trim().toUpperCase(Locale.ROOT)`.
- Thứ tự kiểm tra hợp lệ có tính tất định (deterministic):
  1. Voucher tồn tại trong hệ thống -> `VOUCHER_NOT_FOUND` (404)
  2. Voucher đang kích hoạt (`isActive == true`) -> `VOUCHER_EXPIRED` (400)
  3. Ngày bắt đầu đã đến (`startDate == null || !now.isBefore(startDate)`) -> `VOUCHER_EXPIRED` (400)
  4. Ngày kết thúc chưa qua (`endDate == null || !now.isAfter(endDate)`) -> `VOUCHER_EXPIRED` (400)
  5. Giới hạn sử dụng toàn hệ thống còn hiệu lực (`maxUses == null || usedCount < maxUses`) -> `VOUCHER_ARE_OUT` (400)
  6. Giới hạn sử dụng trên mỗi người dùng còn hiệu lực (`maxPerUser == null || userUsageCount < maxPerUser`) -> `VOUCHER_PER_USER_LIMIT_REACHED` (400)
  7. Giá trị đơn hàng đạt mức tối thiểu (`minOrderValue == null || fullSubtotal.compareTo(minOrderValue) >= 0`) -> `VOUCHER_MIN_ORDER_VALUE_INVALID` (400)
  8. Với scope `SPECIFIC`: Có ít nhất một SKU hợp lệ trong giỏ hàng (`eligibleSubtotal.compareTo(BigDecimal.ZERO) > 0`) -> `VOUCHER_SCOPE_INVALID` (400)
- Công thức tính tiền giảm giá:
  - `FIXED_AMOUNT`: `min(voucher.value, eligibleSubtotal)`
  - `PERCENTAGE`: `eligibleSubtotal * voucher.value / 100` (scale 2, `RoundingMode.HALF_UP`)
  - Nếu có `maxDiscountAmount` > 0: `min(discountAmount, maxDiscountAmount)`
  - Clamp giá trị giảm giá trong khoảng `[0, eligibleSubtotal]`.
  - Nếu voucher không hợp lệ hoặc không có voucher: `discountAmount = 0`.
- Toàn bộ tính toán tiền tệ dùng `BigDecimal`, không dùng `double` hay `float`.

### R-C02-05: Legacy Endpoint Containment
- Đánh dấu `@Deprecated` endpoint `POST /vouchers/validate` (`calculateDiscount`).
- Giữ nguyên DTO/response để tránh break client cũ, nhưng tài liệu hóa rõ không sử dụng endpoint này cho authoritative checkout.
- Cập nhật tài liệu backend chuẩn hóa phạm vi sang `ALL/SPECIFIC`.

## Acceptance Criteria

### AC-C02-01: Single Authoritative Evaluator
Given giỏ hàng và mã voucher của người dùng,
When thực hiện checkout review hoặc create order,
Then cùng một logic tính toán `VoucherCheckoutCalculator` được gọi để xác định tính hợp lệ và số tiền giảm giá.

### AC-C02-02: Min Order Value Threshold Evaluated on Full Subtotal
Given đơn hàng có full subtotal >= `minOrderValue`,
When voucher có `applyScope == SPECIFIC` và eligible subtotal < `minOrderValue`,
Then voucher vẫn được coi là thỏa mãn điều kiện giá trị đơn hàng tối thiểu.

### AC-C02-03: Specific Scope Discount Only on Eligible Subtotal
Given voucher 10% giảm giá với scope `SPECIFIC` cho SKU-A,
When giỏ hàng có SKU-A trị giá 400.000đ và SKU-B trị giá 600.000đ (tổng subtotal 1.000.000đ),
Then số tiền giảm giá là 40.000đ (10% của 400.000đ) chứ không phải 100.000đ.

### AC-C02-04: Specific Scope Inapplicable When No Eligible SKUs
Given voucher có scope `SPECIFIC` cho SKU-A,
When giỏ hàng chỉ có SKU-B,
Then voucher không áp dụng được (`applicable = false`, discount = 0, issue code = `VOUCHER_SCOPE_INVALID`).

### AC-C02-05: Parity of Validation Including Per-User Limit
Given user đã dùng hết số lượt cho phép (`maxPerUser`),
When user gọi `checkoutReview` hoặc `createOrder`,
Then cả 2 đều từ chối voucher với lỗi `VOUCHER_PER_USER_LIMIT_REACHED`.

### AC-C02-06: Money Invariants & Clamping
Given voucher giảm giá cố định 500.000đ,
When eligible subtotal của giỏ hàng là 300.000đ,
Then discount được clamp về tối đa 300.000đ, không làm tổng tiền âm.

### AC-C02-07: Discount Cap for Percentage
Given voucher giảm giá 50% với `maxDiscountAmount = 100.000đ`,
When eligible subtotal là 1.000.000đ,
Then số tiền giảm giá là 100.000đ thay vì 500.000đ.

### AC-C02-08: Case-Insensitive Normalized Code
Given voucher được lưu với code `SUMMER2026`,
When user nhập ` summer2026 `,
Then voucher được normalize thành `SUMMER2026` và xử lý chính xác.

### AC-C02-09: Pure Calculation Without Side Effects
Given evaluator được gọi nhiều lần với cùng context,
Then không có mutation nào tới database (không tăng `usedCount`, không ghi bảng quan hệ user-voucher).

### AC-C02-10: Legacy Endpoint Containment
Given endpoint `POST /vouchers/validate`,
When được gọi,
Then vẫn hoạt động tương thích ngược nhưng được gắn `@Deprecated` và không ảnh hưởng tới checkout flow.

## Non-Goals

- Triển khai partial checkout (chọn một phần giỏ hàng) hoặc cart fingerprinting (sẽ làm trong các plan tiếp theo).
- Thay đổi database schema hoặc cấu trúc bảng `voucher`, `voucher_product_sku`, `voucher_user`.
- Triển khai voucher redemption ledger mới hay xử lý rollback khi hủy đơn hàng.
- Can thiệp giao diện người dùng frontend.

## Edge Cases

- Giỏ hàng rỗng: Đã được chặn trước khi đánh giá voucher (`CART_IS_EMPTY`).
- Voucher có `minOrderValue == null`, `maxDiscountAmount == null`, `maxUses == null`, `maxPerUser == null`: Xử lý null-safe không gây NullPointerException.
- Voucher giảm giá 100% hoặc fixed amount vượt quá tiền hàng: Tiền thanh toán sau giảm giá tối thiểu là 0đ (tiền hàng không âm, phí ship giữ nguyên).

## API & Internal Contract

### Value Object: `VoucherEvaluationContext`
```java
public record VoucherEvaluationContext(
    User user,
    Voucher voucher,
    List<CartLineItem> items,
    BigDecimal fullSubtotal,
    long userUsageCount
) {
    public record CartLineItem(Long skuId, int quantity, BigDecimal unitPrice) {}
}
```

### Value Object: `VoucherEvaluationResult`
```java
public record VoucherEvaluationResult(
    String normalizedCode,
    BigDecimal fullSubtotal,
    BigDecimal eligibleSubtotal,
    BigDecimal discountAmount,
    boolean applicable,
    ErrorCode issueCode,
    Voucher voucher
) {}
```

### Component: `VoucherCheckoutCalculator`
```java
@Component
public class VoucherCheckoutCalculator {
    public VoucherEvaluationResult evaluate(VoucherEvaluationContext context) { ... }
}
```

## Traceability

| Acceptance criterion | Automated evidence | Status |
|---|---|---|
| AC-C02-01 | `OrderServiceTest` checkout review & create order tests | PLANNED |
| AC-C02-02 | `VoucherCheckoutCalculatorTest` min order value on full subtotal | PLANNED |
| AC-C02-03 | `VoucherCheckoutCalculatorTest` specific scope eligible subtotal | PLANNED |
| AC-C02-04 | `VoucherCheckoutCalculatorTest` specific scope no match | PLANNED |
| AC-C02-05 | `OrderServiceTest` per-user limit parity | PLANNED |
| AC-C02-06 | `VoucherCheckoutCalculatorTest` discount clamped to eligible subtotal | PLANNED |
| AC-C02-07 | `VoucherCheckoutCalculatorTest` maxDiscountAmount percentage cap | PLANNED |
| AC-C02-08 | `VoucherCheckoutCalculatorTest` code normalization | PLANNED |
| AC-C02-09 | `VoucherCheckoutCalculatorTest` pure test | PLANNED |
| AC-C02-10 | `VoucherControllerTest` / `VoucherServiceTest` deprecated check | PLANNED |
