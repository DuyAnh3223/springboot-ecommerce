import { notFound } from "next/navigation";
import { getAdminOrderDetail, OrderApiError } from "@/features/orders/services/order.service";
import { getAdminOrderErrorMessage } from "@/features/admin/orders/utils/admin-order-error.utils";
import { AdminOrderDetailView } from "@/features/admin/orders/components/AdminOrderDetailView";
import { Card, CardContent } from "@/components/ui/card";
import { getOrderPayments } from "@/features/payments/services/payment.service";
import { AdminPaymentAttempts } from "@/features/admin/orders/components/AdminPaymentAttempts";

export default async function AdminOrderDetailPage({ params }: { params: Promise<{ orderCode: string }> }) {
  const { orderCode } = await params;
  let order;
  try { order = await getAdminOrderDetail(orderCode); }
  catch (error) {
    if (error instanceof OrderApiError && (error.status === 404 || error.code === 1034)) notFound();
    return <Card><CardContent className="py-12 text-center text-destructive">{getAdminOrderErrorMessage(error)}</CardContent></Card>;
  }
  const payments = await getOrderPayments(orderCode, true).catch(() => null);
  return <div className="space-y-4"><AdminOrderDetailView order={order} />{payments ? <AdminPaymentAttempts payments={payments} /> : <p role="alert">Chưa tải được các lần thanh toán. Vui lòng tải lại trang.</p>}</div>;
}
