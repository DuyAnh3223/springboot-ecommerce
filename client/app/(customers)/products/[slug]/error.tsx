"use client";

import { AlertTriangle, RotateCcw } from "lucide-react";

export default function ProductDetailError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <main className="flex min-h-[65vh] items-center justify-center bg-slate-50 px-4 py-16">
      <div className="max-w-lg rounded-3xl border border-amber-200 bg-white p-8 text-center shadow-sm">
        <AlertTriangle className="mx-auto mb-4 h-12 w-12 text-amber-500" aria-hidden="true" />
        <h1 className="text-2xl font-extrabold text-slate-900">Chưa thể tải sản phẩm</h1>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          Hệ thống đang tạm thời gián đoạn. Vui lòng thử lại sau ít phút.
        </p>
        <button
          type="button"
          onClick={reset}
          className="mt-6 inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white transition hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600"
        >
          <RotateCcw className="h-4 w-4" aria-hidden="true" /> Thử lại
        </button>
      </div>
    </main>
  );
}
