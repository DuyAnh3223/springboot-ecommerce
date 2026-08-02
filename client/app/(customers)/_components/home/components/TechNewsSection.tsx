import { TECH_NEWS } from "../home.config";
import { Clock, Tag, ArrowRight } from "lucide-react";
import Link from "next/link";

export default function TechNewsSection() {
  return (
    <section className="py-12 bg-slate-50 border-b border-slate-200/80 text-slate-900">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-8 pb-4 border-b border-slate-200">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">TIN TỨC & KINH NGHIỆM PC</h2>
            <p className="text-xs text-slate-500 mt-1 font-medium">Cập nhật xu hướng công nghệ, đánh giá linh kiện & hướng dẫn lắp PC</p>
          </div>
          <Link
            href="/category"
            className="text-xs font-bold text-rose-600 hover:text-rose-700 hover:underline transition-colors"
          >
            Xem thêm bài viết &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {TECH_NEWS.map((article) => (
            <article
              key={article.id}
              className="bg-white border border-slate-200/80 rounded-2xl overflow-hidden flex flex-col justify-between hover:border-rose-400 transition-all group hover:shadow-xl shadow-xs"
            >
              <div>
                <div className="relative aspect-16/9 overflow-hidden bg-slate-100">
                  <img
                    src={article.image}
                    alt={article.title}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                  <span className="absolute top-3 left-3 bg-white/95 text-rose-600 border border-slate-200 text-[10px] font-bold px-2.5 py-0.5 rounded-full flex items-center gap-1 backdrop-blur-xs shadow-xs">
                    <Tag className="w-3 h-3" />
                    {article.category}
                  </span>
                </div>

                <div className="p-5 space-y-2">
                  <div className="flex items-center gap-3 text-[11px] text-slate-500 font-medium">
                    <span>{article.date}</span>
                    <span>•</span>
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {article.readTime}
                    </span>
                  </div>

                  <h3 className="text-sm font-bold text-slate-800 line-clamp-2 leading-snug group-hover:text-rose-600 transition-colors">
                    {article.title}
                  </h3>

                  <p className="text-xs text-slate-500 line-clamp-2 leading-relaxed font-normal">
                    {article.excerpt}
                  </p>
                </div>
              </div>

              <div className="p-5 pt-0">
                <Link
                  href="/category"
                  className="inline-flex items-center gap-1.5 text-xs font-bold text-rose-600 hover:text-rose-700 transition-colors group/link"
                >
                  <span>Đọc tiếp</span>
                  <ArrowRight className="w-3.5 h-3.5 group-hover/link:translate-x-1 transition-transform" />
                </Link>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
