"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { buildAdminOrderUrl, isOrderStatus, serializeAdminDate, validateAdminDateRange } from "../utils/admin-order.utils";
import type { AdminOrderListQuery } from "@/features/orders/order.type";

export function AdminOrderFilters({ query }: { query: AdminOrderListQuery }) {
  const router = useRouter();
  function submit(formData: FormData) {
    const from = String(formData.get("fromDate") || "");
    const to = String(formData.get("toDate") || "");
    const rangeError = validateAdminDateRange(from || undefined, to || undefined);
    if (rangeError) { window.alert(rangeError); return; }
    const statusValue = String(formData.get("status") || "");
    router.push(buildAdminOrderUrl({
      search: String(formData.get("search") || "").trim() || undefined,
      status: isOrderStatus(statusValue) ? statusValue : undefined,
      fromDate: serializeAdminDate(from),
      toDate: serializeAdminDate(to, true),
      page: 0,
      size: query.size,
    }));
  }
  return (
    <form action={submit} className="grid gap-3 md:grid-cols-[2fr_1fr_1fr_1fr_auto]">
      <Input name="search" defaultValue={query.search} placeholder="Tìm mã đơn, tên khách hàng, số điện thoại" />
      <select name="status" defaultValue={query.status || ""} className="h-9 rounded-md border bg-background px-3 text-sm">
        <option value="">Tất cả trạng thái</option>
        <option value="PENDING">Chờ xác nhận</option><option value="CONFIRMED">Đã xác nhận</option>
        <option value="SHIPPING">Đang giao</option><option value="DELIVERED">Đã giao</option><option value="CANCELLED">Đã hủy</option>
      </select>
      <Input type="date" name="fromDate" defaultValue={query.fromDate?.slice(0, 10)} aria-label="Từ ngày" />
      <Input type="date" name="toDate" defaultValue={query.toDate?.slice(0, 10)} aria-label="Đến ngày" />
      <Button type="submit">Lọc</Button>
    </form>
  );
}
