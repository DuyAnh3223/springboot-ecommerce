import { BRANDS_LIST } from "../home.config";
import Link from "next/link";

export default function BrandStrip() {
  return (
    <section className="py-8 bg-white border-b border-slate-200/80 text-slate-900 shadow-xs">
      <div className="max-w-7xl mx-auto px-4">
        <div className="text-center mb-6">
          <h3 className="text-xs font-extrabold text-slate-500 uppercase tracking-widest">
            THƯƠNG HIỆU ĐỒNG HÀNH CHÍNH HÃNG TẠI ABTECHZONE
          </h3>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-8 gap-3">
          {BRANDS_LIST.map((brand) => (
            <Link
              key={brand.slug}
              href="/category"
              className="flex items-center justify-center p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 hover:border-rose-400 hover:bg-rose-50/50 text-slate-700 hover:text-rose-600 font-extrabold text-xs tracking-wider transition-all hover:scale-105 shadow-xs"
            >
              <span>{brand.name}</span>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
