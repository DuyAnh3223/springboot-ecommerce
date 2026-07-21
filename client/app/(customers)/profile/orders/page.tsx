import React from "react";
import Link from "next/link";
import { redirect } from "next/navigation";
import { ShoppingBag, Calendar, Receipt, ClipboardCheck } from "lucide-react";
import { getUserSession } from "@/features/auth/actions";
import { getUserOrders } from "@/features/orders/services/order.service";

export const metadata = {
  title: "Đơn hàng đã mua | AB Tech Zone",
};

// Helper function to map backend status to Vietnamese labels and colors
function getStatusBadge(status: string) {
  switch (status?.toUpperCase()) {
    case "PENDING":
      return {
        label: "Chờ xử lý",
        className: "bg-amber-50 text-amber-700 border-amber-200",
      };
    case "CONFIRMED":
      return {
        label: "Đã xác nhận",
        className: "bg-blue-50 text-blue-700 border-blue-200",
      };
    case "SHIPPED":
      return {
        label: "Đang giao hàng",
        className: "bg-indigo-50 text-indigo-700 border-indigo-200",
      };
    case "DELIVERED":
      return {
        label: "Đã giao hàng",
        className: "bg-emerald-50 text-emerald-700 border-emerald-200",
      };
    case "CANCELLED":
      return {
        label: "Đã hủy",
        className: "bg-rose-50 text-rose-700 border-rose-200",
      };
    default:
      return {
        label: status || "Không xác định",
        className: "bg-slate-50 text-slate-700 border-slate-200",
      };
  }
}

// Format currency in VND
function formatVND(amount: number) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount);
}

export default async function ProfileOrdersPage() {
  const user = await getUserSession();

  if (!user) {
    redirect("/sign-in");
  }

  let orders: any[] = [];
  try {
    orders = await getUserOrders(user.id);
  } catch (error) {
    console.error("Failed to load orders on server side:", error);
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-155 p-6 md:p-8 flex flex-col gap-6">
      {/* Header */}
      <div className="pb-4 border-b border-slate-100">
        <h2 className="text-xl font-bold text-slate-850">Đơn hàng đã mua</h2>
        <p className="text-xs text-slate-500 mt-1">
          Theo dõi trạng thái và lịch sử tất cả các đơn đặt hàng của bạn
        </p>
      </div>

      {/* Orders List */}
      {orders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 px-4 border border-dashed border-slate-200 rounded-xl text-slate-400 gap-3">
          <div className="p-3 bg-slate-50 rounded-full text-slate-400 border border-slate-100">
            <ShoppingBag className="size-8 text-slate-350" />
          </div>
          <span className="text-sm font-medium">Bạn chưa thực hiện đơn đặt hàng nào.</span>
          <Link
            href="/shop"
            className="px-5 py-2 rounded-lg bg-shop_orange hover:bg-shop_orange/90 text-white font-semibold text-xs transition-all cursor-pointer shadow-sm hover:shadow-md mt-2"
          >
            Mua sắm ngay
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Desktop Table View */}
          <div className="hidden lg:block overflow-hidden border border-slate-155 rounded-xl">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50/70 border-b border-slate-155 text-xs font-semibold text-slate-500 uppercase">
                  <th className="px-6 py-4">Mã đơn hàng</th>
                  <th className="px-6 py-4">Tổng giá trị</th>
                  <th className="px-6 py-4">Trạng thái</th>
                  <th className="px-6 py-4">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map((order) => {
                  const badge = getStatusBadge(order.orderStatus);
                  return (
                    <tr key={order.orderId} className="hover:bg-slate-50/30 transition-colors">
                      {/* Mã */}
                      <td className="px-6 py-4 font-semibold text-slate-800">
                        {order.orderCode || `#${order.orderId}`}
                      </td>
                      {/* Giá */}
                      <td className="px-6 py-4 font-bold text-shop_orange">
                        {formatVND(order.totalCheckout)}
                      </td>
                      {/* Trạng thái */}
                      <td className="px-6 py-4">
                        <span
                          className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${badge.className}`}
                        >
                          {badge.label}
                        </span>
                      </td>
                      {/* Hành động */}
                      <td className="px-6 py-4">
                        <span className="text-xs text-slate-400 italic">Đang cập nhật chi tiết</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Mobile Card View */}
          <div className="lg:hidden flex flex-col gap-4">
            {orders.map((order) => {
              const badge = getStatusBadge(order.orderStatus);
              return (
                <div
                  key={order.orderId}
                  className="p-5 border border-slate-155 rounded-xl bg-white flex flex-col gap-3 shadow-xs"
                >
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <span className="font-semibold text-slate-800">
                      {order.orderCode || `#${order.orderId}`}
                    </span>
                    <span
                      className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border ${badge.className}`}
                    >
                      {badge.label}
                    </span>
                  </div>
                  <div className="flex justify-between items-center text-sm">
                    <span className="text-slate-500">Tổng giá trị:</span>
                    <span className="font-bold text-shop_orange">
                      {formatVND(order.totalCheckout)}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
