import type { OrderStatus } from "@/features/orders/order.type";
import { ORDER_STATUS_META } from "../utils/order.utils";

interface OrderFiltersProps {
  status?: OrderStatus;
  size: number;
}

export function OrderFilters({ status, size }: OrderFiltersProps) {
  return (
    <form
      action="/profile/orders"
      method="get"
      className="grid gap-3 rounded-xl bg-slate-50 p-4 sm:grid-cols-[minmax(0,1fr)_160px_auto] sm:items-end"
    >
      <label className="space-y-1.5 text-sm font-semibold text-slate-700">
        <span>Trạng thái đơn hàng</span>
        <select
          name="status"
          defaultValue={status ?? ""}
          className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm outline-none focus:border-shop_light_green focus:ring-2 focus:ring-shop_light_green/20"
        >
          <option value="">Tất cả trạng thái</option>
          {(Object.entries(ORDER_STATUS_META) as Array<
            [OrderStatus, (typeof ORDER_STATUS_META)[OrderStatus]]
          >).map(([value, metadata]) => (
            <option key={value} value={value}>
              {metadata.label}
            </option>
          ))}
        </select>
      </label>

      <label className="space-y-1.5 text-sm font-semibold text-slate-700">
        <span>Số đơn mỗi trang</span>
        <select
          name="size"
          defaultValue={String(size)}
          className="h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm outline-none focus:border-shop_light_green focus:ring-2 focus:ring-shop_light_green/20"
        >
          <option value="10">10 đơn</option>
          <option value="20">20 đơn</option>
          <option value="50">50 đơn</option>
        </select>
      </label>

      <button
        type="submit"
        className="inline-flex h-10 items-center justify-center rounded-lg bg-slate-900 px-5 text-sm font-bold text-white transition hover:bg-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-shop_light_green"
      >
        Áp dụng
      </button>
    </form>
  );
}
