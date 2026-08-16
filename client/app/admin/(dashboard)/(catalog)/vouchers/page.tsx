import { getVouchers } from "@/features/vouchers/services/voucher.service";
import { VouchersClient } from "@/features/admin/catalog/vouchers/components/VouchersClient";

interface PageProps {
  searchParams: Promise<{
    search?: string;
    status?: string;
    active?: string;
    page?: string;
    size?: string;
    sortBy?: string;
    order?: string;
  }>;
}

export const metadata = {
  title: "Quản lý mã giảm giá | Admin",
  description: "Quản lý mã voucher giảm giá, thời hạn và khuyến mãi.",
};

export default async function VouchersPage({ searchParams }: PageProps) {
  const params = await searchParams;

  const search = params.search || undefined;
  const status = params.status as "active" | "expired" | "disabled" | undefined;
  const active = params.active ? params.active === "true" : undefined;
  const page = params.page ? parseInt(params.page) : 1;
  const size = params.size ? parseInt(params.size) : 10;
  const sortBy = params.sortBy || "id";
  const order = (params.order as "asc" | "desc") || "desc";

  let initialData;
  try {
    initialData = await getVouchers({
      search,
      status,
      active,
      page,
      size,
      sortBy,
      order,
    });
  } catch (error) {
    console.error("Lỗi khi tải danh sách voucher:", error);
    initialData = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true,
    };
  }

  return (
    <div className="space-y-2 animate-in fade-in duration-300 mx-auto">
      <VouchersClient initialData={initialData} />
    </div>
  );
}
