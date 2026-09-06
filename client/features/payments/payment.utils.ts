import type { OrderPayments } from "./payment.type";

export function paymentMessage(data: OrderPayments): string {
  if (data.attempts.some(p => p.applicationStatus === "REVIEW_REQUIRED" || p.reviewReason))
    return "Thanh toán cần được kiểm tra. Vui lòng liên hệ cửa hàng và không thanh toán thêm.";
  if (data.paymentStatus === "PAID") return "Đã nhận thanh toán.";
  if (data.orderStatus === "CANCELLED") return "Đơn hàng đã huỷ. Nếu tài khoản đã bị trừ tiền, vui lòng liên hệ cửa hàng.";
  if (data.attempts.some(p => p.initiationState === "UNKNOWN"))
    return "Đang kiểm tra kết quả với cổng thanh toán. Vui lòng không thanh toán thêm.";
  if (data.retryAllowed) return "Lần thanh toán trước chưa thành công. Bạn có thể thử lại.";
  return "Đơn hàng đang chờ thanh toán.";
}
export function canOpenPayment(data: OrderPayments): boolean {
  if (data.orderStatus !== "PENDING" || data.paymentStatus === "PAID"
      || data.attempts.some(p => p.applicationStatus === "REVIEW_REQUIRED" || p.reviewReason)) return false;
  const latest = data.attempts.at(-1);
  return Boolean(data.retryAllowed || (latest?.status === "PENDING" && ["NEW", "READY"].includes(latest.initiationState || "")));
}
