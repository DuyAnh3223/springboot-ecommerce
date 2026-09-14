# Commerce-16 — Phí vận chuyển GHN và quản lý giao hàng

**Status:** Accepted by explicit user requirement on 2026-09-11.

## Scope

ABTechZone dùng GHN như một dịch vụ báo giá vận chuyển. Backend gọi duy nhất fee API của GHN; không tạo vận đơn, không webhook, không đồng bộ trạng thái, không đối soát và không thanh toán cho GHN. Phí trả về được cộng vào `Order.shippingFee` và `Order.totalAmount`; shop thu toàn bộ tổng tiền đơn.

GHN nhận một trọng lượng quy ước cố định do backend cấu hình cho mọi đơn. Không đọc trọng lượng, kích thước hay quy cách đóng gói từ SKU. Báo giá vẫn dựa trên tuyến kho → địa chỉ nhận hàng và mã tuyến GHN.

Admin tự cập nhật giao hàng qua state machine hiện có:

`PENDING -> CONFIRMED -> SHIPPING -> DELIVERED | DELIVERY_FAILED`

`SHIPPING` là đang giao và tương ứng action “Bắt đầu giao”. `DELIVERED` là giao thành công. `DELIVERY_FAILED` là giao thất bại và bắt buộc ghi chú lý do. Hai state cuối là terminal trong MVP; không tự hoàn kho, hoàn tiền hoặc giao lại khi thất bại. ETA/thông báo đến hạn là phạm vi sau, chưa triển khai trong MVP này.

## Requirements

- **R1 — GHN fee quote:** `POST /orders/checkout-review` và `POST /orders` phải dùng server-side GHN fee theo `addressId` hoặc địa chỉ mới (XOR). Credentials, origin route, service type và fixed weight chỉ lấy từ backend config.
- **R2 — Server-owned money:** Checkout review và order creation phải dùng cùng công thức server; client không được quyết định `shippingFee` hoặc `totalAmount`. Nếu fee/tuyến thay đổi sau review, trả `CHECKOUT_CHANGED` và không tạo order.
- **R3 — Failure handling:** Thiếu mã tuyến trả lỗi địa chỉ; GHN timeout, HTTP lỗi, application code khác 200 hoặc fee không hợp lệ trả `SHIPPING_PROVIDER_UNAVAILABLE`; không âm thầm fallback 30000.
- **R4 — Admin delivery:** Chỉ admin được cập nhật trạng thái qua `PATCH /admin/orders/{orderCode}/status`. Chỉ cho phép các transition trong policy; `DELIVERY_FAILED` cần note không rỗng, tối đa 500 ký tự.
- **R5 — Payment/inventory:** Chuyển `SHIPPING -> DELIVERED` giữ hành vi COD hiện có (`markCodSucceeded`). Chuyển `SHIPPING -> DELIVERY_FAILED` chỉ ghi order/history; không mark paid, refund, tăng/giảm tồn hoặc voucher.
- **R6 — Read model/UI:** Customer và admin hiển thị `DELIVERY_FAILED`, allowed transitions và order history hiện có. Checkout hiển thị fee GHN trả về; không có màn hình kết nối GHN hay tracking code GHN.

## Acceptance criteria

- **AC1:** Với destination route hợp lệ, fee request gửi đúng origin, destination, service type và fixed weight; `shippingFee` và payment amount bằng giá trị server tính.
- **AC2:** Không có token/shop/origin/route hoặc GHN lỗi thì checkout không tạo Order/Payment và frontend hiển thị hướng dẫn thử lại.
- **AC3:** Cùng idempotency key/payload replay kết quả cũ, không gọi lại GHN hay tạo side effect lần hai.
- **AC4:** Đổi địa chỉ hoặc fee sau review trả 409 `CHECKOUT_CHANGED`.
- **AC5:** Admin chỉ chuyển được `CONFIRMED -> SHIPPING`, `SHIPPING -> DELIVERED` hoặc `SHIPPING -> DELIVERY_FAILED`; failure không có note bị từ chối.
- **AC6:** Customer không được gọi admin mutation; terminal state không được chuyển tiếp.
- **AC7:** Automated tests cover GHN request/response mapping, provider failure, fixed weight, fee validation và delivery failure side effects. Human UAT remains pending.

## Out of scope

ETA, scheduler/notification, shipment entity/repository, tracking timeline riêng, fulfillment API, webhook/inbox, cancellation/refund/retry delivery và settlement với GHN.

## Addendum — GHN address dropdown

The following requirements were added by explicit user request on 2026-09-11 and are part of the accepted implementation scope:

- **D1 — Shared address picker:** Checkout and the Address module use one dependent Province → District → Ward picker. The picker displays names but submits GHN IDs; `WardCode` remains a string.
- **D2 — Server-owned address data:** Address create/update resolves the selected GHN route server-side and stores canonical province/district/ward names with the route IDs. Client-provided names are display hints only.
- **D3 — Legacy addresses:** Existing addresses remain readable. An address without a complete valid GHN route cannot be used for a new checkout quote until the customer reselects and saves the route.
- **D4 — Location API boundary:** The backend exposes authenticated location read endpoints and keeps GHN credentials out of the browser. Provider failures return a typed 503; an empty valid list is distinct from a malformed/provider failure response.
- **D5 — Quote invalidation:** Changing any shipping address field invalidates the old review, even when the resulting fee is numerically equal. Only the latest address selection may be submitted.
- **D6 — Partial address update:** Updating only default/recipient/contact/street fields must preserve route fields. Clearing or sending an incomplete route is rejected; no fuzzy matching or destructive backfill is allowed.

Acceptance criteria for the addendum:

- **AC-D1:** The shared picker loads dependent lists, resets child selections when a parent changes, preserves ward codes as strings, and ignores stale responses after a newer selection.
- **AC-D2:** Address create/edit stores a server-resolved route; invalid parent/child combinations are rejected; default-only updates do not call GHN or erase route data.
- **AC-D3:** Checkout uses the same picker, disables submission while the quote is missing/stale/loading, and sends only the selected route IDs to review/create.
- **AC-D4:** Legacy addresses can be displayed and edited; no route is guessed from names.
- **AC-D5:** Location endpoints require authentication, do not expose Token/ShopId, and map timeout, HTTP, malformed, and provider application errors to 503.
- **AC-D6:** An address change with the same fee still produces a new authoritative review requirement and cannot replay a stale create payload.
- **AC-D7:** Automated tests cover location client mapping/errors, route membership, Address partial updates, picker reset/race behavior, and checkout invalidation.
