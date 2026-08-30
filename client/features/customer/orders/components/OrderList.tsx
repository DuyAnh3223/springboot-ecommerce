import Link from "next/link";
import { PackageOpen, ReceiptText } from "lucide-react";
import type { OrderSummaryResponse } from "@/features/orders/order.type";
import { formatCurrency } from "@/shared/utils";
import {
  formatOrderDate,
  getPaymentMethodLabel,
  getPaymentStatusLabel,
} from "../utils/order.utils";
import { OrderStatusBadge } from "./OrderStatusBadge";

interface OrderListProps {
  orders: OrderSummaryResponse[];
}

export function OrderList({ orders }: OrderListProps) {
  if (orders.length === 0) {
    return (
      <div className="flex flex-col items-center rounded-xl border border-dashed border-slate-200 px-4 py-14 text-center">
        <PackageOpen className="h-10 w-10 text-slate-300" />
        <p className="mt-3 font-bold text-slate-800">Chưa có đơn hàng phù hợp</p>
        <p className="mt-1 text-sm text-slate-500">
          Hãy thử trạng thái khác hoặc tiếp tục mua sắm tại ABTechZone.
        </p>
        <Link
          href="/"
          className="mt-5 inline-flex h-10 items-center rounded-lg bg-shop_light_green px-5 text-sm font-bold text-white hover:bg-shop_dark_green"
        >
          Tiếp tục mua sắm
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {orders.map((order) => (
        <article
          key={order.id}
          className="overflow-hidden rounded-xl border border-slate-200 transition hover:border-slate-300 hover:shadow-sm"
        >
          <div className="flex flex-col gap-3 border-b border-slate-100 bg-slate-50/70 px-4 py-4 sm:flex-row sm:items-center sm:justify-between md:px-5">
            <div>
              <Link
                href={`/profile/orders/${encodeURIComponent(order.orderCode)}`}
                className="font-black text-slate-900 hover:text-shop_dark_green"
              >
                {order.orderCode}
              </Link>
              <p className="mt-1 text-xs text-slate-500">
                Đặt lúc {formatOrderDate(order.createdAt)}
              </p>
            </div>
            <OrderStatusBadge status={order.status} />
          </div>

          <div className="grid gap-4 p-4 text-sm md:grid-cols-[minmax(0,1fr)_220px_auto] md:items-center md:p-5">
            <div className="flex min-w-0 items-start gap-3">
              <div className="rounded-lg bg-emerald-50 p-2 text-shop_dark_green">
                <ReceiptText className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <p className="truncate font-bold text-slate-800">
                  {order.previewItem?.productName ?? "Thông tin sản phẩm đã được lưu trong đơn"}
                </p>
                <p className="mt-1 text-xs text-slate-500">
                  {order.itemCount} sản phẩm
                  {order.previewItem ? ` · ${order.previewItem.quantity} × ${formatCurrency(order.previewItem.unitPrice)}` : ""}
                </p>
              </div>
            </div>

            <div className="text-slate-600">
              <p>{getPaymentMethodLabel(order.paymentMethod)}</p>
              <p className="mt-1 text-xs font-semibold">
                {getPaymentStatusLabel(order.paymentStatus)}
              </p>
            </div>

            <div className="flex items-center justify-between gap-4 md:block md:text-right">
              <div>
                <p className="text-xs text-slate-500">Tổng thanh toán</p>
                <p className="mt-1 font-black text-shop_orange">
                  {formatCurrency(order.totalAmount)}
                </p>
              </div>
              <Link
                href={`/profile/orders/${encodeURIComponent(order.orderCode)}`}
                className="inline-flex h-9 items-center rounded-lg border border-slate-200 px-4 text-xs font-bold text-slate-700 hover:border-shop_light_green hover:text-shop_dark_green"
              >
                Xem chi tiết
              </Link>
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
