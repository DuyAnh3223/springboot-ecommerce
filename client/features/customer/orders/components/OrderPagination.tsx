import Link from "next/link";
import type { OrderStatus } from "@/features/orders/order.type";
import { buildOrderListUrl } from "../utils/order.utils";

interface OrderPaginationProps {
  currentPage: number;
  totalPages: number;
  status?: OrderStatus;
  size: number;
}

export function OrderPagination({
  currentPage,
  totalPages,
  status,
  size,
}: OrderPaginationProps) {
  const safeCurrentPage = Math.min(Math.max(currentPage, 1), totalPages);
  const previousUrl = buildOrderListUrl({
    status,
    page: Math.max(1, safeCurrentPage - 1),
    size,
  });
  const nextUrl = buildOrderListUrl({
    status,
    page: Math.min(totalPages, safeCurrentPage + 1),
    size,
  });

  return (
    <nav
      aria-label="Phân trang đơn hàng"
      className="flex flex-col items-center justify-between gap-3 border-t border-slate-100 pt-5 sm:flex-row"
    >
      <p className="text-sm text-slate-600">
        Trang <span className="font-bold text-slate-900">{safeCurrentPage}</span> / {totalPages}
      </p>
      <div className="flex gap-2">
        {safeCurrentPage > 1 ? (
          <Link
            href={previousUrl}
            className="inline-flex h-9 items-center rounded-lg border border-slate-200 px-4 text-sm font-bold text-slate-700 hover:bg-slate-50"
          >
            Trang trước
          </Link>
        ) : (
          <span className="inline-flex h-9 cursor-not-allowed items-center rounded-lg border border-slate-100 px-4 text-sm font-bold text-slate-300">
            Trang trước
          </span>
        )}
        {safeCurrentPage < totalPages ? (
          <Link
            href={nextUrl}
            className="inline-flex h-9 items-center rounded-lg bg-slate-900 px-4 text-sm font-bold text-white hover:bg-slate-800"
          >
            Trang sau
          </Link>
        ) : (
          <span className="inline-flex h-9 cursor-not-allowed items-center rounded-lg bg-slate-100 px-4 text-sm font-bold text-slate-300">
            Trang sau
          </span>
        )}
      </div>
    </nav>
  );
}
