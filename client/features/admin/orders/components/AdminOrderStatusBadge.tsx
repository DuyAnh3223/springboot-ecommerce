import { Badge } from "@/components/ui/badge";
import type { OrderStatus } from "@/features/orders/order.type";
import { ADMIN_ORDER_STATUS_META } from "../utils/admin-order.utils";

export function AdminOrderStatusBadge({ status }: { status: OrderStatus }) {
  const meta = ADMIN_ORDER_STATUS_META[status];
  return <Badge className={meta.className}>{meta.label}</Badge>;
}
