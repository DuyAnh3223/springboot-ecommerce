import type { ReactNode } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  CreditCard,
  MapPin,
  PackageCheck,
  ReceiptText,
  Tag,
} from "lucide-react";
import type { OrderDetailResponse } from "@/features/orders/order.type";
import { formatCurrency } from "@/shared/utils";
import {
  formatOrderDate,
  getPaymentMethodLabel,
  getPaymentStatusLabel,
} from "../utils/order.utils";
import { OrderStatusBadge } from "./OrderStatusBadge";
import { OrderTimeline } from "./OrderTimeline";

interface OrderDetailViewProps {
  order: OrderDetailResponse;
  actions?: ReactNode;
}

export function OrderDetailView({ order, actions }: OrderDetailViewProps) {
  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-8">
        <Link
          href="/profile/orders"
          className="inline-flex items-center gap-2 text-sm font-bold text-slate-600 hover:text-shop_dark_green"
        >
          <ArrowLeft className="h-4 w-4" /> Quay lại danh sách đơn hàng
        </Link>

        <div className="mt-5 flex flex-col gap-4 border-b border-slate-100 pb-6 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-shop_light_green">
              Chi tiết đơn hàng
            </p>
            <h1 className="mt-1 text-2xl font-black text-slate-900">{order.orderCode}</h1>
            <p className="mt-2 text-sm text-slate-500">
              Đặt lúc {formatOrderDate(order.createdAt)}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <OrderStatusBadge status={order.status} />
            {actions}
          </div>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <div className="rounded-xl border border-slate-100 p-4">
            <div className="flex items-center gap-2 font-bold text-slate-800">
              <CreditCard className="h-5 w-5 text-shop_light_green" /> Thanh toán
            </div>
            <p className="mt-3 text-sm text-slate-600">
              {getPaymentMethodLabel(order.paymentMethod)}
            </p>
            <p className="mt-1 text-sm font-semibold text-slate-700">
              {getPaymentStatusLabel(order.paymentStatus)}
            </p>
          </div>

          <div className="rounded-xl border border-slate-100 p-4">
            <div className="flex items-center gap-2 font-bold text-slate-800">
              <MapPin className="h-5 w-5 text-shop_light_green" /> Địa chỉ nhận hàng
            </div>
            <p className="mt-3 text-sm font-semibold text-slate-700">
              {order.recipientName} · {order.phone}
            </p>
            <p className="mt-1 text-sm text-slate-600">{order.fullAddress}</p>
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.5fr)_minmax(320px,0.7fr)]">
        <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
          <div className="flex items-center gap-2 border-b border-slate-100 pb-4">
            <PackageCheck className="h-5 w-5 text-shop_light_green" />
            <h2 className="text-lg font-black text-slate-900">Sản phẩm đã đặt</h2>
          </div>
          <div className="divide-y divide-slate-100">
            {order.items.map((item) => (
              <article key={item.skuId} className="grid gap-3 py-5 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
                <div className="flex min-w-0 items-start gap-3">
                  <div className="rounded-lg bg-slate-50 p-2 text-slate-500">
                    <ReceiptText className="h-5 w-5" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-bold text-slate-800">{item.productName}</p>
                    <p className="mt-1 text-xs text-slate-500">
                      {item.skuCode} · Số lượng {item.quantity}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      Đơn giá {formatCurrency(item.unitPrice)}
                    </p>
                  </div>
                </div>
                <p className="font-black text-shop_orange">
                  {formatCurrency(item.lineTotal)}
                </p>
              </article>
            ))}
          </div>
        </section>

        <div className="space-y-6">
          <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
            <h2 className="text-lg font-black text-slate-900">Tổng thanh toán</h2>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex justify-between gap-4 text-slate-600">
                <dt>Tạm tính</dt>
                <dd>{formatCurrency(order.subtotalAmount)}</dd>
              </div>
              <div className="flex justify-between gap-4 text-slate-600">
                <dt>Phí vận chuyển</dt>
                <dd>{formatCurrency(order.shippingFee)}</dd>
              </div>
              <div className="flex justify-between gap-4 text-slate-600">
                <dt>Giảm giá</dt>
                <dd>-{formatCurrency(order.discountAmount)}</dd>
              </div>
              {order.voucherCode && (
                <div className="flex items-center justify-between gap-4 rounded-lg bg-emerald-50 px-3 py-2 text-emerald-800">
                  <dt className="flex items-center gap-1.5"><Tag className="h-4 w-4" /> Voucher</dt>
                  <dd className="font-bold">{order.voucherCode}</dd>
                </div>
              )}
              <div className="flex justify-between gap-4 border-t border-slate-100 pt-3 text-base font-black text-slate-900">
                <dt>Tổng cộng</dt>
                <dd className="text-shop_orange">{formatCurrency(order.totalAmount)}</dd>
              </div>
            </dl>
          </section>

          <section className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
            <h2 className="mb-5 text-lg font-black text-slate-900">Lịch sử đơn hàng</h2>
            <OrderTimeline history={order.history} />
          </section>
        </div>
      </div>
    </div>
  );
}
