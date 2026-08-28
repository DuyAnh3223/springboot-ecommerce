import Link from "next/link";
import { PackageSearch } from "lucide-react";

export default function ProductNotFound() {
  return (
    <main className="flex min-h-[65vh] items-center justify-center bg-slate-50 px-4 py-16">
      <div className="max-w-lg rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <PackageSearch className="mx-auto mb-4 h-12 w-12 text-emerald-600" aria-hidden="true" />
        <h1 className="text-2xl font-extrabold text-slate-900">Không tìm thấy sản phẩm</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Sản phẩm không tồn tại hoặc hiện chưa được mở bán. Bạn có thể quay lại danh mục để chọn sản phẩm khác.
        </p>
        <Link
          href="/"
          className="mt-6 inline-flex rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600"
        >
          Quay về trang chủ
        </Link>
      </div>
    </main>
  );
}
