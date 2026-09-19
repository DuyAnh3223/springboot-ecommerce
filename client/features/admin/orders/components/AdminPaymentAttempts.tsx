import type { OrderPayments } from "@/features/payments/payment.type";

const statuses: Record<string, string> = { PENDING: "Chờ thanh toán", SUCCEEDED: "Đã thu tiền", FAILED: "Không thành công", CANCELLED: "Đã huỷ" };
export function AdminPaymentAttempts({ payments }: { payments: OrderPayments }) {
  return <section className="space-y-3 rounded-xl border p-4"><h2 className="font-semibold">Các lần thanh toán</h2>
    {payments.attempts.map(p => <div key={p.id} className="border-b pb-3 text-sm last:border-0">
      <p>#{p.id} · {p.provider} · {statuses[p.status] || "Cần kiểm tra"} · {p.amount.toLocaleString("vi-VN")} đ</p>
      <p>Mã giao dịch: {p.providerReference || "Chưa có"}</p>
      {(p.applicationStatus === "REVIEW_REQUIRED" || p.reviewReason) && <p className="font-medium text-amber-800">Cần kiểm tra với cổng thanh toán: {p.reviewReason === "LATE_SUCCESS" ? "Tiền về sau khi đơn hết hạn hoặc đã huỷ." : p.reviewReason === "EXTRA_PAYMENT" ? "Có thêm khoản thanh toán cho đơn." : "Kết quả hoặc số tiền cần đối chiếu."} Chưa tự động hoàn tiền.</p>}
    </div>)}
  </section>;
}
