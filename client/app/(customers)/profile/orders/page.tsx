import { redirect } from "next/navigation";
import { OrderApiError, getMyOrders } from "@/features/orders/services/order.service";
import { getOrderErrorMessage } from "@/features/orders/order-error";
import { OrderFilters } from "@/features/customer/orders/components/OrderFilters";
import { OrderList } from "@/features/customer/orders/components/OrderList";
import { OrderPagination } from "@/features/customer/orders/components/OrderPagination";
import { parseOrderListQuery } from "@/features/customer/orders/utils/order.utils";

interface ProfileOrdersPageProps {
  searchParams: Promise<{
    status?: string | string[];
    page?: string | string[];
    size?: string | string[];
  }>;
}

export const metadata = {
  title: "Đơn hàng đã mua | ABTechZone",
  description: "Theo dõi lịch sử và trạng thái đơn hàng của bạn tại ABTechZone.",
};

export default async function ProfileOrdersPage({
  searchParams,
}: ProfileOrdersPageProps) {
  const query = parseOrderListQuery(await searchParams);
  let ordersPage = null;
  let loadError: string | null = null;

  try {
    ordersPage = await getMyOrders({
      status: query.status,
      page: query.apiPage,
      size: query.size,
    });
  } catch (error: unknown) {
    if (error instanceof OrderApiError && error.status === 401) {
      redirect("/sign-in?callbackUrl=%2Fprofile%2Forders");
    }

    loadError = getOrderErrorMessage(
      error instanceof OrderApiError
        ? { code: error.code, status: error.status }
        : {},
    );
  }

  return (
    <section className="space-y-6 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-8">
      <header className="border-b border-slate-100 pb-5">
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-shop_light_green">
          Tài khoản của tôi
        </p>
        <h1 className="mt-1 text-2xl font-black text-slate-900">Đơn hàng đã mua</h1>
        <p className="mt-2 text-sm text-slate-600">
          Theo dõi trạng thái, thanh toán và thông tin giao hàng của từng đơn.
        </p>
      </header>

      <OrderFilters status={query.status} size={query.size} />

      {loadError ? (
        <div
          role="alert"
          className="rounded-xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-800"
        >
          <p className="font-bold">Chưa thể tải danh sách đơn hàng</p>
          <p className="mt-1">{loadError}</p>
        </div>
      ) : (
        <>
          <OrderList orders={ordersPage?.content ?? []} />
          {ordersPage && ordersPage.totalPages > 1 && (
            <OrderPagination
              currentPage={query.page}
              totalPages={ordersPage.totalPages}
              status={query.status}
              size={query.size}
            />
          )}
        </>
      )}
    </section>
  );
}
