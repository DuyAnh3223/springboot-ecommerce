import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { CancelOrderDialog } from "@/features/customer/orders/components/CancelOrderDialog";
import { OrderDetailView } from "@/features/customer/orders/components/OrderDetailView";
import { getOrderErrorMessage, isOrderNotFound } from "@/features/orders/order-error";
import { getOrderDetail, OrderApiError } from "@/features/orders/services/order.service";

interface CustomerOrderDetailPageProps {
  params: Promise<{ orderCode: string }>;
}

export const metadata = {
  title: "Chi tiết đơn hàng | ABTechZone",
};

export default async function CustomerOrderDetailPage({
  params,
}: CustomerOrderDetailPageProps) {
  const { orderCode: rawOrderCode } = await params;
  const orderCode = rawOrderCode.trim();
  if (!orderCode) notFound();

  let order = null;
  let loadError: string | null = null;

  try {
    order = await getOrderDetail(orderCode);
  } catch (error: unknown) {
    if (error instanceof OrderApiError) {
      if (isOrderNotFound({ code: error.code, status: error.status })) {
        notFound();
      }
      if (error.status === 401) {
        redirect(
          `/sign-in?callbackUrl=${encodeURIComponent(`/profile/orders/${orderCode}`)}`,
        );
      }
      loadError = getOrderErrorMessage({ code: error.code, status: error.status });
    } else {
      loadError = getOrderErrorMessage({});
    }
  }

  if (!order) {
    return (
      <section className="space-y-5 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-8">
        <Link
          href="/profile/orders"
          className="inline-flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-shop_dark_green"
        >
          <ArrowLeft className="h-4 w-4" /> Quay lại danh sách đơn hàng
        </Link>
        <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-800">
          <p className="font-bold">Chưa thể tải chi tiết đơn hàng</p>
          <p className="mt-1">{loadError}</p>
        </div>
      </section>
    );
  }

  return (
    <OrderDetailView
      order={order}
      actions={(
        <CancelOrderDialog
          orderCode={order.orderCode}
          allowedTransitions={order.allowedTransitions}
        />
      )}
    />
  );
}
