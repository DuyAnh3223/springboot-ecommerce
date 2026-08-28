import type {
  OrderHistoryResponse,
  OrderStatus,
} from "@/features/orders/order.type";

export interface OrderStatusMeta {
  label: string;
  className: string;
  progress: number;
}

export const ORDER_STATUS_META: Record<OrderStatus, OrderStatusMeta> = {
  PENDING: {
    label: "Chờ xác nhận",
    className: "border-amber-200 bg-amber-50 text-amber-700",
    progress: 1,
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    className: "border-blue-200 bg-blue-50 text-blue-700",
    progress: 2,
  },
  SHIPPING: {
    label: "Đang giao hàng",
    className: "border-indigo-200 bg-indigo-50 text-indigo-700",
    progress: 3,
  },
  DELIVERED: {
    label: "Đã giao hàng",
    className: "border-emerald-200 bg-emerald-50 text-emerald-700",
    progress: 4,
  },
  CANCELLED: {
    label: "Đã hủy",
    className: "border-rose-200 bg-rose-50 text-rose-700",
    progress: 0,
  },
};

const ORDER_STATUSES = new Set<OrderStatus>(Object.keys(ORDER_STATUS_META) as OrderStatus[]);
const PAGE_SIZES = new Set([10, 20, 50]);

export interface OrderListSearchParams {
  status?: string | string[];
  page?: string | string[];
  size?: string | string[];
}

export interface ParsedOrderListQuery {
  status?: OrderStatus;
  page: number;
  apiPage: number;
  size: number;
}

function firstValue(value?: string | string[]): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function parsePositiveInteger(value?: string): number | undefined {
  if (!value || !/^\d+$/.test(value)) return undefined;
  const parsed = Number.parseInt(value, 10);
  return parsed > 0 ? parsed : undefined;
}

export function parseOrderListQuery(
  params: OrderListSearchParams,
): ParsedOrderListQuery {
  const rawStatus = firstValue(params.status)?.toUpperCase();
  const status = rawStatus && ORDER_STATUSES.has(rawStatus as OrderStatus)
    ? (rawStatus as OrderStatus)
    : undefined;
  const page = parsePositiveInteger(firstValue(params.page)) ?? 1;
  const rawSize = parsePositiveInteger(firstValue(params.size));
  const size = rawSize && PAGE_SIZES.has(rawSize) ? rawSize : 10;

  return { status, page, apiPage: page - 1, size };
}

export function buildOrderListUrl(query: {
  status?: OrderStatus;
  page: number;
  size: number;
}): string {
  const params = new URLSearchParams();
  if (query.status) params.set("status", query.status);
  if (query.page > 1) params.set("page", String(query.page));
  if (query.size !== 10) params.set("size", String(query.size));
  const search = params.toString();
  return search ? `/profile/orders?${search}` : "/profile/orders";
}

export function canCancelOrder(allowedTransitions: OrderStatus[]): boolean {
  return allowedTransitions.includes("CANCELLED");
}

export function getHistoryTargetStatus(
  history: Pick<OrderHistoryResponse, "toStatus" | "status">,
): OrderStatus | null {
  return history.toStatus ?? history.status;
}

export function getPaymentMethodLabel(paymentMethod: string): string {
  return paymentMethod === "COD" ? "Thanh toán khi nhận hàng (COD)" : "Phương thức thanh toán";
}

export function getPaymentStatusLabel(paymentStatus: string): string {
  switch (paymentStatus) {
    case "UNPAID":
      return "Chưa thanh toán";
    case "PAID":
      return "Đã thanh toán";
    case "CANCELLED":
      return "Đã hủy thanh toán";
    default:
      return "Đang cập nhật";
  }
}

export function formatOrderDate(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date(value));
}
