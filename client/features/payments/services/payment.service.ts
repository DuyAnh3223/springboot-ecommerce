import "server-only";
import { api } from "@/shared/http/api";
import type { OnlineProvider, OrderPayments, PaymentCheckout } from "../payment.type";

export async function getPaymentProviders(): Promise<OnlineProvider[]> {
  const response = await api.get("/payments/providers");
  return response.data.result;
}
export async function getOrderPayments(orderCode: string, admin = false): Promise<OrderPayments> {
  const response = await api.get(`${admin ? "/admin" : ""}/orders/${encodeURIComponent(orderCode)}/payments`);
  return response.data.result;
}
export async function createPaymentCheckout(orderCode: string, provider: OnlineProvider, key: string): Promise<PaymentCheckout> {
  const response = await api.post(`/orders/${encodeURIComponent(orderCode)}/payments/checkout`, { provider }, {
    headers: { "Idempotency-Key": key }, timeout: 45000,
  });
  return response.data.result;
}
