export default function ProductDetailLoading() {
  return (
    <main className="min-h-screen bg-slate-50 pb-16" aria-busy="true" aria-label="Đang tải sản phẩm">
      <div className="mx-auto max-w-7xl animate-pulse px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-6 h-4 w-72 rounded bg-slate-200" />
        <div className="grid gap-8 rounded-3xl border border-slate-200 bg-white p-5 lg:grid-cols-2 lg:p-8">
          <div className="aspect-square rounded-2xl bg-slate-200" />
          <div className="space-y-5 py-2">
            <div className="h-4 w-28 rounded bg-slate-200" />
            <div className="h-9 w-4/5 rounded bg-slate-200" />
            <div className="h-7 w-48 rounded bg-slate-200" />
            <div className="h-24 rounded-2xl bg-slate-100" />
            <div className="h-12 rounded-xl bg-slate-200" />
          </div>
        </div>
      </div>
    </main>
  );
}
