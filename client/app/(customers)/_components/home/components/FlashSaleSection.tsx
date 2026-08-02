"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Flame, ShoppingCart } from "lucide-react";
import { FLASH_SALE_CAMPAIGN } from "../home.config";

export default function FlashSaleSection() {
  const [timeLeft, setTimeLeft] = useState({
    hours: 11,
    minutes: 45,
    seconds: 30,
  });

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) return { ...prev, seconds: prev.seconds - 1 };
        if (prev.minutes > 0) return { ...prev, minutes: prev.minutes - 1, seconds: 59 };
        if (prev.hours > 0) return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        return prev;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatVND = (price: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

  const formatNum = (num: number) => String(num).padStart(2, "0");

  return (
    <section id="flash-sale" className="py-10 bg-slate-50 border-b border-slate-200/80 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4">
        {/* Header Bar */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-200">
          <div className="flex items-center gap-3">
            <span className="p-2.5 rounded-xl bg-rose-600 text-white shadow-md">
              <Flame className="w-6 h-6 animate-bounce" />
            </span>
            <div>
              <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900 flex items-center gap-2">
                FLASH SALE GIỜ VÀNG
              </h2>
              <p className="text-xs text-slate-500 font-medium">Số lượng có hạn - Săn deal giá sốc hôm nay</p>
            </div>
          </div>

          <div className="flex items-center gap-2 bg-white px-4 py-2 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-xs text-slate-600 font-semibold mr-1">Kết thúc sau:</span>
            <div className="flex items-center gap-1 font-mono text-xs font-bold text-white">
              <span className="bg-rose-600 px-2 py-1 rounded text-white shadow-xs">{formatNum(timeLeft.hours)}</span>
              <span className="text-rose-600 font-bold">:</span>
              <span className="bg-rose-600 px-2 py-1 rounded text-white shadow-xs">{formatNum(timeLeft.minutes)}</span>
              <span className="text-rose-600 font-bold">:</span>
              <span className="bg-rose-600 px-2 py-1 rounded text-white shadow-xs">{formatNum(timeLeft.seconds)}</span>
            </div>
          </div>
        </div>

        {/* Product Cards Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
          {FLASH_SALE_CAMPAIGN.map((item) => {
            const soldPercent = Math.min(100, Math.round((item.soldCount / item.totalStock) * 100));
            return (
              <div
                key={item.id}
                className="bg-white border border-slate-200/80 rounded-2xl p-4 flex flex-col justify-between hover:border-rose-400 transition-all group hover:shadow-xl shadow-xs"
              >
                <div>
                  {/* Thumbnail Container */}
                  <div className="relative aspect-4/3 rounded-xl overflow-hidden bg-slate-100 mb-3 border border-slate-200/60">
                    <img
                      src={item.thumbnail}
                      alt={item.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                    <span className="absolute top-2 left-2 bg-rose-600 text-white text-[11px] font-black px-2.5 py-0.5 rounded-md shadow-md">
                      -{item.discountPercent}%
                    </span>
                  </div>

                  <span className="text-[11px] font-semibold text-rose-600 uppercase tracking-wider block mb-1">
                    {item.categoryName}
                  </span>

                  <h3 className="text-xs sm:text-sm font-bold text-slate-800 line-clamp-2 mb-2 group-hover:text-rose-600 transition-colors">
                    {item.name}
                  </h3>
                </div>

                <div className="space-y-3 pt-2">
                  <div className="flex items-baseline gap-2">
                    <span className="text-sm sm:text-base font-extrabold text-rose-600">
                      {formatVND(item.salePrice)}
                    </span>
                    <span className="text-xs text-slate-400 line-through">
                      {formatVND(item.originalPrice)}
                    </span>
                  </div>

                  {/* Progress Bar */}
                  <div className="space-y-1">
                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden border border-slate-200/60">
                      <div
                        className="h-full bg-gradient-to-r from-rose-500 to-amber-500 rounded-full transition-all duration-500"
                        style={{ width: `${soldPercent}%` }}
                      />
                    </div>
                    <div className="flex items-center justify-between text-[10px] text-slate-500 font-medium">
                      <span>🔥 Đã bán: {item.soldCount}</span>
                      <span>Còn {item.totalStock - item.soldCount} suất</span>
                    </div>
                  </div>

                  <Link
                    href={`/category/${item.productSlug}`}
                    className="w-full inline-flex items-center justify-center gap-1.5 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-md"
                  >
                    <ShoppingCart className="w-3.5 h-3.5" />
                    <span>Săn Deal Ngay</span>
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
