import Link from "next/link";
import { CategoryTile } from "../types";
import { getSafeImageUrl } from "@/lib/utils";
import { Cpu, Fan, CircuitBoard, MemoryStick, HardDrive, Monitor, Zap, Box, LayoutGrid } from "lucide-react";

interface CategoryGridProps {
  categories: CategoryTile[];
}

const iconMap: Record<string, React.ReactNode> = {
  cpu: <Cpu className="w-7 h-7 text-rose-500" />,
  "cpu-cooler": <Fan className="w-7 h-7 text-cyan-400" />,
  motherboard: <CircuitBoard className="w-7 h-7 text-indigo-400" />,
  ram: <MemoryStick className="w-7 h-7 text-amber-400" />,
  storage: <HardDrive className="w-7 h-7 text-emerald-400" />,
  vga: <Monitor className="w-7 h-7 text-purple-400" />,
  psu: <Zap className="w-7 h-7 text-yellow-400" />,
  case: <Box className="w-7 h-7 text-rose-400" />,
};

function getCategoryIcon(slug: string) {
  const normalized = slug.toLowerCase();
  for (const [key, icon] of Object.entries(iconMap)) {
    if (normalized.includes(key)) return icon;
  }
  return <LayoutGrid className="w-7 h-7 text-rose-400" />;
}

export default function CategoryGrid({ categories }: CategoryGridProps) {
  return (
    <section className="py-10 bg-white border-b border-slate-200/80 text-slate-900 shadow-xs">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between mb-6 pb-3 border-b border-slate-200">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">DANH MỤC LINH KIỆN NỔI BẬT</h2>
            <p className="text-xs text-slate-500 mt-1 font-medium">Linh kiện máy tính & phụ kiện chính hãng chọn lọc</p>
          </div>
          <Link
            href="/category"
            className="text-xs font-bold text-rose-600 hover:text-rose-700 hover:underline transition-colors"
          >
            Xem tất cả &rarr;
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-8 gap-3 sm:gap-4">
          {categories.map((cat) => {
            const safeImg = getSafeImageUrl(cat.imageUrl);
            return (
              <Link
                key={cat.slug}
                href={`/category/${cat.slug}`}
                className="group flex flex-col items-center justify-center p-4 rounded-2xl bg-slate-50/80 border border-slate-200/80 hover:bg-white hover:border-rose-400 hover:shadow-lg transition-all hover:-translate-y-1 text-center"
              >
                <div className="w-12 h-12 rounded-xl bg-white border border-slate-200 flex items-center justify-center mb-2.5 group-hover:scale-110 group-hover:border-rose-300 transition-all shadow-xs">
                  {safeImg ? (
                    <img src={safeImg} alt={cat.title} className="w-8 h-8 object-contain" />
                  ) : (
                    getCategoryIcon(cat.slug)
                  )}
                </div>
                <span className="text-xs font-bold text-slate-800 group-hover:text-rose-600 line-clamp-2 transition-colors">
                  {cat.title}
                </span>
              </Link>
            );
          })}
        </div>
      </div>
    </section>
  );
}
