"use server";
import { createPaymentCheckout } from "../services/payment.service";
import type { OnlineProvider } from "../payment.type";

export async function startPaymentAction(orderCode: string, provider: OnlineProvider, key: string) {
  try { return { success: true as const, data: await createPaymentCheckout(orderCode, provider, key) }; }
  catch { return { success: false as const, message: "Chưa xác định được kết quả thanh toán. Vui lòng kiểm tra lại trạng thái đơn trước khi thử thanh toán tiếp." }; }
}
