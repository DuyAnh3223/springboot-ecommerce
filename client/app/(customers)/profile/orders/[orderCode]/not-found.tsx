import Link from "next/link";
import { PackageX } from "lucide-react";

export default function CustomerOrderNotFound() {
  return (
    <section className="rounded-2xl border border-slate-100 bg-white p-8 text-center shadow-sm md:p-12">
      <PackageX className="mx-auto h-12 w-12 text-slate-300" />
      <h1 className="mt-4 text-2xl font-black text-slate-900">Không tìm thấy đơn hàng</h1>
      <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-600">
        Đơn hàng không tồn tại hoặc bạn không có quyền xem thông tin này.
      </p>
      <Link
        href="/profile/orders"
        className="mt-6 inline-flex h-10 items-center rounded-lg bg-shop_light_green px-5 text-sm font-bold text-white hover:bg-shop_dark_green"
      >
        Quay lại đơn hàng của tôi
      </Link>
    </section>
  );
}
