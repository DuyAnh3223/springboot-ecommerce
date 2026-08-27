import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { CheckCircle2, MapPin, PackageCheck } from "lucide-react";
import { getUserSession } from "@/features/auth/actions";
import { getOrderDetail } from "@/features/orders/services/order.service";
import { formatCurrency } from "@/shared/utils";
import { buildSignInCallbackUrl } from "@/features/customer/checkout/utils/checkout.utils";

interface CheckoutSuccessPageProps {
  searchParams: Promise<{ orderCode?: string }>;
}

export const metadata = {
  title: "Đặt hàng thành công | ABTechZone",
};

export default async function CheckoutSuccessPage({
  searchParams,
}: CheckoutSuccessPageProps) {
  const params = await searchParams;
  const orderCode = params.orderCode?.trim();
  if (!orderCode) notFound();

  const session = await getUserSession();
  if (!session) {
    redirect(buildSignInCallbackUrl(`/checkout/success?orderCode=${encodeURIComponent(orderCode)}`));
  }

  let order;
  try {
    order = await getOrderDetail(orderCode);
  } catch {
    notFound();
  }

  return (
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="rounded-2xl border border-emerald-200 bg-white p-6 text-center shadow-sm md:p-10">
          <CheckCircle2 className="mx-auto h-16 w-16 text-emerald-500" />
          <p className="mt-5 text-sm font-bold uppercase tracking-[0.18em] text-emerald-600">
            Đặt hàng thành công
          </p>
          <h1 className="mt-2 text-3xl font-black text-slate-900">Cảm ơn bạn đã mua hàng</h1>
          <p className="mt-3 text-sm text-slate-600">
            Đơn hàng của bạn đã được ghi nhận. Vui lòng thanh toán khi nhận hàng.
          </p>

          <div className="mt-8 grid gap-3 text-left sm:grid-cols-2">
            <div className="rounded-xl bg-slate-50 p-4">
              <p className="text-xs font-semibold text-slate-500">Mã đơn hàng</p>
              <p className="mt-1 font-black text-slate-900">{order.orderCode}</p>
            </div>
            <div className="rounded-xl bg-slate-50 p-4">
              <p className="text-xs font-semibold text-slate-500">Tổng thanh toán</p>
              <p className="mt-1 font-black text-shop_dark_green">{formatCurrency(order.totalAmount)}</p>
            </div>
          </div>

          <div className="mt-4 space-y-3 rounded-xl border border-slate-100 p-4 text-left text-sm">
            <div className="flex items-start gap-3">
              <PackageCheck className="mt-0.5 h-5 w-5 shrink-0 text-shop_light_green" />
              <div>
                <p className="font-bold text-slate-800">Thanh toán COD</p>
                <p className="mt-1 text-slate-600">Trạng thái: {order.paymentStatus}</p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <MapPin className="mt-0.5 h-5 w-5 shrink-0 text-shop_light_green" />
              <div>
                <p className="font-bold text-slate-800">Địa chỉ nhận hàng</p>
                <p className="mt-1 text-slate-600">
                  {order.recipientName} · {order.phone}
                </p>
                <p className="mt-1 text-slate-600">{order.fullAddress}</p>
              </div>
            </div>
          </div>

          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link
              href="/"
              className="inline-flex h-10 items-center justify-center rounded-lg bg-shop_light_green px-5 text-sm font-bold text-white hover:bg-shop_dark_green"
            >
              Tiếp tục mua sắm
            </Link>
            <Link
              href="/profile/orders"
              className="inline-flex h-10 items-center justify-center rounded-lg border border-slate-200 px-5 text-sm font-bold text-slate-700 hover:bg-slate-50"
            >
              Xem đơn hàng của tôi
            </Link>
          </div>
        </div>
      </div>
  );
}
