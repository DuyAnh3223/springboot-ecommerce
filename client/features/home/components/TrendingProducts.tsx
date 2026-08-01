"use client";

import { useState } from "react";
import { CatalogProductItem } from "@/features/(catalog)/catalog/types/catalog.types";
import HomeProductCard from "./HomeProductCard";
import ProductQuickView from "./ProductQuickView";
import { Flame, Sparkles, Tag, ArrowRight } from "lucide-react";
import Link from "next/link";

interface TrendingProductsProps {
  products: CatalogProductItem[];
  isLoading: boolean;
}

type TabType = "bestSeller" | "newArrival" | "bestDeal";

export default function TrendingProducts({ products, isLoading }: TrendingProductsProps) {
  const [activeTab, setActiveTab] = useState<TabType>("bestSeller");
  const [quickViewProduct, setQuickViewProduct] = useState<CatalogProductItem | null>(null);

  // Filter products or slice based on tab
  const getFilteredProducts = () => {
    if (!products || products.length === 0) return [];
    if (activeTab === "bestSeller") {
      return [...products].sort((a, b) => (b.reviewCount || 0) - (a.reviewCount || 0)).slice(0, 8);
    }
    if (activeTab === "newArrival") {
      return [...products].reverse().slice(0, 8);
    }
    if (activeTab === "bestDeal") {
      return [...products].sort((a, b) => a.priceMin - b.priceMin).slice(0, 8);
    }
    return products.slice(0, 8);
  };

  const displayProducts = getFilteredProducts();

  return (
    <section className="py-12 bg-slate-50 border-b border-slate-200/80 text-slate-900">
      <div className="max-w-7xl mx-auto px-4">
        {/* Header & Tabs */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8 pb-4 border-b border-slate-200">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-slate-900">SẢN PHẨM LINH KIỆN HOT</h2>
            <p className="text-xs text-slate-500 mt-1 font-medium">Linh kiện máy tính được cộng đồng Gaming tin dùng nhất</p>
          </div>

          <div className="flex items-center gap-1.5 bg-slate-200/60 p-1.5 rounded-2xl border border-slate-200 shadow-inner">
            <button
              type="button"
              onClick={() => setActiveTab("bestSeller")}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                activeTab === "bestSeller"
                  ? "bg-rose-600 text-white shadow-md"
                  : "text-slate-600 hover:text-slate-900 hover:bg-white"
              }`}
            >
              <Flame className="w-3.5 h-3.5" />
              <span>Bán Chạy</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab("newArrival")}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                activeTab === "newArrival"
                  ? "bg-rose-600 text-white shadow-md"
                  : "text-slate-600 hover:text-slate-900 hover:bg-white"
              }`}
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>Mới Về</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab("bestDeal")}
              className={`flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                activeTab === "bestDeal"
                  ? "bg-rose-600 text-white shadow-md"
                  : "text-slate-600 hover:text-slate-900 hover:bg-white"
              }`}
            >
              <Tag className="w-3.5 h-3.5" />
              <span>Giá Tốt</span>
            </button>
          </div>
        </div>

        {/* Product Grid / Skeleton */}
        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {[...Array(8)].map((_, idx) => (
              <div
                key={idx}
                className="bg-white border border-slate-200 rounded-2xl p-4 h-72 animate-pulse flex flex-col justify-between shadow-xs"
              >
                <div className="w-full h-36 bg-slate-100 rounded-xl mb-3" />
                <div className="h-4 bg-slate-100 rounded w-3/4 mb-2" />
                <div className="h-4 bg-slate-100 rounded w-1/2" />
              </div>
            ))}
          </div>
        ) : displayProducts.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            {displayProducts.map((prod) => (
              <HomeProductCard key={prod.id} product={prod} onQuickView={setQuickViewProduct} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 bg-white border border-slate-200 rounded-2xl p-6 shadow-xs">
            <p className="text-slate-500 text-sm mb-3 font-medium">Hiện chưa có sản phẩm trực tuyến trong danh mục này.</p>
            <Link
              href="/category"
              className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-md"
            >
              <span>Xem Tất Cả Danh Mục</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        )}
      </div>

      {/* Quick View Modal */}
      <ProductQuickView product={quickViewProduct} onClose={() => setQuickViewProduct(null)} />
    </section>
  );
}
