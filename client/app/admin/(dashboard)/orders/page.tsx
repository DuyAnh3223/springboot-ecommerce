import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { getAdminOrders } from "@/features/orders/services/order.service";
import { getAdminOrderErrorMessage } from "@/features/admin/orders/utils/admin-order-error.utils";
import { AdminOrderFilters } from "@/features/admin/orders/components/AdminOrderFilters";
import { AdminOrderList } from "@/features/admin/orders/components/AdminOrderList";
import { parseAdminOrderQuery, validateAdminDateRange } from "@/features/admin/orders/utils/admin-order.utils";

export default async function OrdersPage({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const raw = await searchParams;
  const params = new URLSearchParams();
  Object.entries(raw).forEach(([key, value]) => { if (typeof value === "string") params.set(key, value); });
  const query = parseAdminOrderQuery(params);
  const dateError = validateAdminDateRange(query.fromDate?.slice(0, 10), query.toDate?.slice(0, 10));
  if (dateError) return <Card><CardContent className="py-12 text-center text-destructive">{dateError}</CardContent></Card>;
  let result;
  try { result = await getAdminOrders(query); }
  catch (error) { return <Card><CardContent className="py-12 text-center text-destructive">{getAdminOrderErrorMessage(error)}</CardContent></Card>; }
  return <div className="space-y-4"><div><h1 className="text-2xl font-semibold">Quản lý đơn hàng</h1><p className="text-sm text-muted-foreground">Theo dõi và cập nhật trạng thái đơn hàng.</p></div><Card><CardHeader><CardTitle>Bộ lọc</CardTitle></CardHeader><CardContent><AdminOrderFilters query={query} /></CardContent></Card><AdminOrderList {...result} search={query.search} status={query.status} fromDate={query.fromDate} toDate={query.toDate} size={query.size} /></div>;
}
