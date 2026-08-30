import type {
  AdminOrderListQuery,
  OrderStatus,
} from "@/features/orders/order.type";

export const ADMIN_ORDER_PAGE_SIZES = [10, 20, 50] as const;

export const ADMIN_ORDER_STATUS_META: Record<
  OrderStatus,
  { label: string; className: string }
> = {
  PENDING: { label: "Chờ xác nhận", className: "bg-amber-100 text-amber-800" },
  CONFIRMED: { label: "Đã xác nhận", className: "bg-blue-100 text-blue-800" },
  SHIPPING: { label: "Đang giao", className: "bg-indigo-100 text-indigo-800" },
  DELIVERED: { label: "Đã giao", className: "bg-emerald-100 text-emerald-800" },
  CANCELLED: { label: "Đã hủy", className: "bg-rose-100 text-rose-800" },
};

export const ADMIN_ORDER_STATUSES = Object.keys(ADMIN_ORDER_STATUS_META) as OrderStatus[];

export const isOrderStatus = (value: string | null | undefined): value is OrderStatus =>
  value !== undefined && value !== null && ADMIN_ORDER_STATUSES.includes(value as OrderStatus);

export function normalizeAdminNote(note: string): string {
  return note.trim().slice(0, 500);
}

export function serializeAdminDate(value: string, endOfDay = false): string | undefined {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return undefined;
  return `${value}T${endOfDay ? "23:59:59.999" : "00:00:00.000"}Z`;
}

export function parseAdminOrderQuery(params: URLSearchParams): AdminOrderListQuery {
  const search = params.get("search")?.trim() || undefined;
  const statusParam = params.get("status");
  const status = isOrderStatus(statusParam) ? statusParam : undefined;
  const fromDate = params.get("fromDate") || undefined;
  const toDate = params.get("toDate") || undefined;
  const requestedSize = Number(params.get("size"));
  const size = ADMIN_ORDER_PAGE_SIZES.includes(requestedSize as (typeof ADMIN_ORDER_PAGE_SIZES)[number])
    ? requestedSize
    : 20;
  const page = Math.max(0, Number.parseInt(params.get("page") || "0", 10) || 0);
  return { search, status, fromDate, toDate, page, size };
}

export function validateAdminDateRange(fromDate?: string, toDate?: string): string | null {
  if (fromDate && toDate && fromDate > toDate) return "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
  return null;
}

export function buildAdminOrderUrl(query: AdminOrderListQuery): string {
  const params = new URLSearchParams();
  if (query.search?.trim()) params.set("search", query.search.trim());
  if (query.status) params.set("status", query.status);
  if (query.fromDate) params.set("fromDate", query.fromDate);
  if (query.toDate) params.set("toDate", query.toDate);
  if (query.page > 0) params.set("page", String(query.page));
  if (query.size !== 20) params.set("size", String(query.size));
  const queryString = params.toString();
  return queryString ? `/admin/orders?${queryString}` : "/admin/orders";
}

export function getAllowedStatusActions(allowedTransitions: OrderStatus[]) {
  return allowedTransitions.filter(isOrderStatus).map((status) => ({
    status,
    label: ADMIN_ORDER_STATUS_META[status].label,
  }));
}

export function formatAdminMoney(value: number): string {
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
}

export function formatAdminDate(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
