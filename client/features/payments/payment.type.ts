export type OnlineProvider = "MOMO" | "VNPAY" | "PAYOS";
export interface PaymentAttempt {
  id: number;
  provider: string;
  status: "PENDING" | "SUCCEEDED" | "FAILED" | "CANCELLED";
  amount: number;
  paidAt: string | null;
  providerReference: string | null;
  initiationState: string | null;
  applicationStatus: string | null;
  reviewReason: string | null;
  paymentDeadline: string | null;
}
export interface OrderPayments {
  orderCode: string;
  orderStatus: string;
  paymentMethod: string;
  paymentStatus: string;
  retryAllowed: boolean;
  attempts: PaymentAttempt[];
}
export interface PaymentCheckout {
  paymentId: number;
  checkoutUrl: string | null;
  status: string;
  initiationState: string;
  applicationStatus: string;
  reviewReason: string | null;
  deadline: string;
}
