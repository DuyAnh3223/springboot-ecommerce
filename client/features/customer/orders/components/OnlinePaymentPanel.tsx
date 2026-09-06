"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import { startPaymentAction } from "@/features/payments/actions/payment.action";
import { canOpenPayment, paymentMessage } from "@/features/payments/payment.utils";
import type { OnlineProvider, OrderPayments } from "@/features/payments/payment.type";

export function OnlinePaymentPanel({ payments, providers }: { payments: OrderPayments; providers: OnlineProvider[] }) {
  const router = useRouter();
  const key = useRef<string | null>(null);
  const locked = useRef(false);
  const { run, isLoading } = useAsyncAction();
  const [message, setMessage] = useState<string | null>(null);
  const [retryProvider, setRetryProvider] = useState<OnlineProvider | "">("");
  const latest = payments.attempts.at(-1);
  const provider = payments.retryAllowed ? retryProvider || providers[0] : latest?.provider;
  const canPay = canOpenPayment(payments) && providers.includes(provider as OnlineProvider);
  const polling = payments.orderStatus === "PENDING" && payments.paymentStatus !== "PAID"
    && !payments.attempts.some(p => p.applicationStatus === "REVIEW_REQUIRED");

  useEffect(() => {
    if (!polling) return;
    let count = 0;
    const timer = setInterval(() => { if (++count <= 12) router.refresh(); else clearInterval(timer); }, 5000);
    return () => clearInterval(timer);
  }, [polling, router]);

  async function pay() {
    if (locked.current || !canPay || !provider) return;
    locked.current = true;
    key.current ||= crypto.randomUUID();
    const result = await run(() => startPaymentAction(payments.orderCode, provider as OnlineProvider, key.current!));
    if (result?.success && result.data.checkoutUrl) { window.location.assign(result.data.checkoutUrl); return; }
    setMessage(result?.success ? "Đang kiểm tra thanh toán. Trạng thái sẽ được cập nhật từ hệ thống." : result?.message || "Chưa thể kết nối. Vui lòng kiểm tra lại trạng thái.");
    router.refresh(); locked.current = false;
  }
  return <section className="space-y-3 rounded-xl border bg-white p-4" aria-label="Thanh toán trực tuyến">
    <h2 className="font-semibold">Thanh toán trực tuyến</h2>
    <p role="status" className="text-sm">{paymentMessage(payments)}</p>
    {latest?.paymentDeadline && <p className="text-sm text-muted-foreground">Hạn thanh toán: {new Date(latest.paymentDeadline).toLocaleString("vi-VN")}</p>}
    {payments.retryAllowed && <label className="block text-sm">Cổng thanh toán
      <select value={provider || ""} onChange={e => { setRetryProvider(e.target.value as OnlineProvider); key.current = null; }} className="ml-2 rounded border p-2">
        {providers.map(p => <option key={p} value={p}>{p === "MOMO" ? "MoMo" : p === "PAYOS" ? "payOS" : "VNPAY"}</option>)}
      </select>
    </label>}
    <div className="flex gap-2">
      {canPay && <Button disabled={isLoading} onClick={pay}>{isLoading ? "Đang mở thanh toán…" : "Tiếp tục thanh toán"}</Button>}
      <Button variant="outline" onClick={() => router.refresh()}>Kiểm tra trạng thái</Button>
    </div>
    {message && <p className="text-sm" role="status">{message}</p>}
  </section>;
}
