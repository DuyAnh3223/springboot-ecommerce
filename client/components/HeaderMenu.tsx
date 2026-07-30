"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { headerData, categoriesData } from "@/constants/data";
import { getCategories } from "@/features/(catalog)/categories/services/category.service";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { ChevronDown, Cpu, Fan, CircuitBoard, MemoryStick, HardDrive, Monitor, Zap, Box, Folder } from "lucide-react";
import { getSafeImageUrl } from "@/lib/utils";
import { useOutsideClick } from "@/shared/hooks";

const categoryIconMap: Record<string, React.ReactNode> = {
  cpu: <Cpu className="w-5 h-5 text-shop_light_green" />,
  "cpu-cooler": <Fan className="w-5 h-5 text-cyan-600" />,
  motherboard: <CircuitBoard className="w-5 h-5 text-indigo-600" />,
  ram: <MemoryStick className="w-5 h-5 text-amber-600" />,
  storage: <HardDrive className="w-5 h-5 text-shop_light_green" />,
  vga: <Monitor className="w-5 h-5 text-purple-600" />,
  psu: <Zap className="w-5 h-5 text-yellow-600" />,
  case: <Box className="w-5 h-5 text-rose-600" />,
};

function getCategoryIcon(slug: string) {
  const normalized = slug.toLowerCase();
  for (const [key, icon] of Object.entries(categoryIconMap)) {
    if (normalized.includes(key)) return icon;
  }
  return <Folder className="w-5 h-5 text-shop_light_green" />;
}

const HeaderMenu = () => {
  const pathname = usePathname();
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isOpen, setIsOpen] = useState(false);

  const menuRef = useOutsideClick<HTMLDivElement>(() => setIsOpen(false));

  useEffect(() => {
    let isMounted = true;
    getCategories({ size: 50, isActive: true })
      .then((res) => {
        if (isMounted && res?.content) {
          setCategories(res.content);
        }
      })
      .catch((err) => {
        console.warn("Không thể tải danh mục từ API:", err);
      });
    return () => {
      isMounted = false;
    };
  }, []);

  const displayCategories =
    categories.length > 0
      ? categories.map((c) => ({
          title: c.name,
          href: c.slug,
          imageUrl: c.thumbnailUrl || c.thumbnail,
        }))
      : categoriesData.map((c) => ({
          title: c.title,
          href: c.href,
          imageUrl: null,
        }));

  return (
    <div className="hidden md:inline-flex items-center gap-7 text-sm font-semibold text-lightColor relative">
      {headerData?.map((item) => {
        const isCategory = item.href === "/category";
        const isActive =
          pathname === item.href || (isCategory && pathname?.startsWith("/category"));

        if (isCategory) {
          return (
            <div key={item.title} ref={menuRef} className="relative">
              <button
                type="button"
                onClick={() => setIsOpen((prev) => !prev)}
                className={`hover:text-shop_light_green hoverEffect relative flex items-center gap-1 py-2 cursor-pointer outline-none ${
                  isActive ? "text-shop_light_green font-bold" : ""
                }`}
              >
                <span>{item.title}</span>
                <ChevronDown
                  className={`w-4 h-4 transition-transform duration-200 ${
                    isOpen ? "rotate-180 text-shop_light_green" : ""
                  }`}
                />
                <span
                  className={`absolute -bottom-0.5 left-1/2 w-0 h-0.5 bg-shop_light_green group-hover:w-1/2 hoverEffect group-hover:left-0 ${
                    isActive ? "w-1/2" : ""
                  }`}
                />
                <span
                  className={`absolute -bottom-0.5 right-1/2 w-0 h-0.5 bg-shop_light_green group-hover:w-1/2 hoverEffect group-hover:right-0 ${
                    isActive ? "w-1/2" : ""
                  }`}
                />
              </button>

              {/* Mega Dropdown Menu */}
              {isOpen && (
                <div className="absolute top-full left-1/2 -translate-x-1/2 pt-2 w-[520px] z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                  <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-2xl ring-1 ring-black/5">
                    <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-100">
                      <span className="text-xs font-bold uppercase tracking-wider text-shop_light_green">
                        Danh mục linh kiện PC
                      </span>
                      <Link
                        href="/category"
                        onClick={() => setIsOpen(false)}
                        className="text-xs text-slate-500 hover:text-shop_light_green hoverEffect font-medium"
                      >
                        Xem tất cả &rarr;
                      </Link>
                    </div>

                    <div className="grid grid-cols-2 gap-2.5">
                      {displayCategories.map((cat) => {
                        const safeImg = getSafeImageUrl(cat.imageUrl);
                        return (
                          <Link
                            key={cat.href}
                            href={`/category/${cat.href}`}
                            onClick={() => setIsOpen(false)}
                            className="flex items-center gap-3 p-2.5 rounded-xl bg-slate-50/80 border border-slate-150 hover:bg-emerald-50/60 hover:border-shop_light_green/40 group/cat hoverEffect"
                          >
                            <div className="w-9 h-9 rounded-lg bg-white flex items-center justify-center border border-slate-200 shadow-xs group-hover/cat:border-shop_light_green group-hover/cat:scale-105 hoverEffect flex-shrink-0 overflow-hidden">
                              {safeImg ? (
                                <img
                                  src={safeImg}
                                  alt={cat.title}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                getCategoryIcon(cat.href)
                              )}
                            </div>
                            <div className="flex flex-col">
                              <span className="text-xs font-semibold text-slate-800 group-hover/cat:text-shop_light_green hoverEffect line-clamp-1">
                                {cat.title}
                              </span>
                            </div>
                          </Link>
                        );
                      })}
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        }

        return (
          <Link
            key={item.title}
            href={item.href}
            className={`hover:text-shop_light_green hoverEffect relative group py-2 ${
              isActive ? "text-shop_light_green font-bold" : ""
            }`}
          >
            {item.title}
            <span
              className={`absolute -bottom-0.5 left-1/2 w-0 h-0.5 bg-shop_light_green group-hover:w-1/2 hoverEffect group-hover:left-0 ${
                isActive ? "w-1/2" : ""
              }`}
            />
            <span
              className={`absolute -bottom-0.5 right-1/2 w-0 h-0.5 bg-shop_light_green group-hover:w-1/2 hoverEffect group-hover:right-0 ${
                isActive ? "w-1/2" : ""
              }`}
            />
          </Link>
        );
      })}
    </div>
  );
};

export default HeaderMenu;