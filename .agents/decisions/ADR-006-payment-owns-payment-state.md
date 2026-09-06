# ADR-006 — Payment Owns Payment State

## Status

Accepted

Accepted by: user, 2026-09-05.

## Context

Order đang giữ `payment_method` và `payment_status`, nhưng Payment cần ghi nhận
nhiều attempts, provider reference và thời điểm thanh toán. Lưu cùng lifecycle
ở cả hai aggregate sẽ tạo duplication và không thể biểu diễn retry rõ ràng.

## Decision

- Payment là source of truth cho method, status, amount, provider reference và
  paid_at; một Order có nhiều Payment attempts.
- Order giữ payable amount/currency và fulfillment state. API Order vẫn trả
  payment summary được dẫn xuất từ Payment để tương thích contract COD.
- Tạo Order tạo initial Payment trong cùng transaction. COD delivery/cancel cập
  nhật Payment trong transaction Order lifecycle hiện hữu.
- Payment success không làm giảm Inventory lần nữa. ADR-003 và ADR-005 vẫn áp
  dụng: Order create giảm stock, cancellation hoàn stock.
- Payment mutations lock Order trước để serialize cancel, delivery, retry và
  payment result. Commerce-15 (accepted 2026-09-06) mở rộng gateway/adapter và
  reconciliation; refund và outbox vẫn ngoài phạm vi.

## Consequences

- Bỏ persisted `Order.payment_method` và `Order.payment_status` khi cutover
  schema dev sạch.
- Mọi Order mới phải có Payment; thiếu Payment là integrity error, không fallback.
- Commerce-15 thay runtime mock bằng gateway thật; mock controller còn ở test source.
- Merchant attempt được persist trước HTTP; network nằm ngoài DB/Redis locks.
  Callback/query khóa Order và refresh entity khi đi qua nhiều transaction trong
  cùng HTTP request. Financial status tách khỏi application outcome; nullable
  unique applied_order_id bảo vệ một payment áp dụng vào Order.
- payment_checkout_key là idempotency receipt, không sở hữu lifecycle. Nó ánh xạ
  mọi accepted request key vào attempt, kể cả initial hoặc active attempt được reuse.
