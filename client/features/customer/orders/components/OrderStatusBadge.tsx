import type { OrderStatus } from "@/features/orders/order.type";
import { ORDER_STATUS_META } from "../utils/order.utils";

interface OrderStatusBadgeProps {
  status: OrderStatus;
}

export function OrderStatusBadge({ status }: OrderStatusBadgeProps) {
  const metadata = ORDER_STATUS_META[status];

  return (
    <span
      className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${metadata.className}`}
    >
      {metadata.label}
    </span>
  );
}
