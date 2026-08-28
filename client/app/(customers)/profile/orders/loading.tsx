export default function CustomerOrdersLoading() {
  return (
    <section
      aria-busy="true"
      aria-live="polite"
      className="space-y-5 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-8"
    >
      <span className="sr-only">Đang tải đơn hàng...</span>
      <div className="h-7 w-56 animate-pulse rounded bg-slate-100" />
      <div className="h-20 animate-pulse rounded-xl bg-slate-50" />
      {[1, 2, 3].map((item) => (
        <div key={item} className="space-y-3 rounded-xl border border-slate-100 p-5">
          <div className="h-5 w-44 animate-pulse rounded bg-slate-100" />
          <div className="h-4 w-full animate-pulse rounded bg-slate-50" />
          <div className="h-4 w-2/3 animate-pulse rounded bg-slate-50" />
        </div>
      ))}
    </section>
  );
}
