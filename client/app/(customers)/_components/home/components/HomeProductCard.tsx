"use client";

import { CatalogProductItem } from "@/features/customer/catalog/types/catalog.types";
import { getSafeImageUrl } from "@/shared/utils/image";
import { Star, Eye, ShoppingCart } from "lucide-react";
import Link from "next/link";

interface HomeProductCardProps {
  product: CatalogProductItem;
  onQuickView: (product: CatalogProductItem) => void;
}

export default function HomeProductCard({ product, onQuickView }: HomeProductCardProps) {
  const safeImg = getSafeImageUrl(product.thumbnail || product.primaryImageUrl);

  const formatVND = (price: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

  const rating = product.rating || 5;

  return (
    <div className="bg-white border border-slate-200/80 rounded-2xl p-4 flex flex-col justify-between hover:border-rose-400 transition-all group hover:shadow-xl shadow-xs relative">
      <div>
        {/* Product Image */}
        <div className="relative aspect-square rounded-xl overflow-hidden bg-slate-100 mb-3 border border-slate-200/60">
          {safeImg ? (
            <img
              src={safeImg}
              alt={product.name}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-slate-400 text-xs font-semibold">
              ABTechZone
            </div>
          )}

          {/* Quick View Button Overlay */}
          <button
            type="button"
            onClick={() => onQuickView(product)}
            className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 text-xs font-bold text-white backdrop-blur-xs"
          >
            <Eye className="w-4 h-4 text-rose-400" />
            <span>Xem Nhanh</span>
          </button>

          {/* Brand Badge */}
          {product.brand?.name && (
            <span className="absolute top-2 left-2 bg-white/90 text-slate-800 text-[10px] font-bold px-2 py-0.5 rounded border border-slate-200 shadow-xs">
              {product.brand.name}
            </span>
          )}
        </div>

        {/* Title */}
        <h3 className="text-xs sm:text-sm font-bold text-slate-800 line-clamp-2 mb-1.5 group-hover:text-rose-600 transition-colors">
          {product.name}
        </h3>

        {/* Rating */}
        <div className="flex items-center gap-1 mb-2">
          <div className="flex text-amber-400">
            {[...Array(5)].map((_, i) => (
              <Star
                key={i}
                className={`w-3 h-3 ${i < Math.floor(rating) ? "fill-amber-400 text-amber-400" : "text-slate-200"}`}
              />
            ))}
          </div>
          <span className="text-[10px] text-slate-500 font-medium">({product.reviewCount || 12})</span>
        </div>
      </div>

      {/* Footer / Price & Action */}
      <div className="pt-2 border-t border-slate-100 space-y-2">
        <div className="flex flex-col">
          <span className="text-[10px] text-slate-500 font-medium">GiÃ¡ niÃªm yáº¿t:</span>
          <span className="text-sm font-extrabold text-rose-600">
            {formatVND(product.priceMin)}
            {product.priceMax > product.priceMin && ` - ${formatVND(product.priceMax)}`}
          </span>
        </div>

        <div className="flex items-center gap-2 pt-1">
          <button
            type="button"
            onClick={() => onQuickView(product)}
            className="flex-1 py-2 px-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold transition-all border border-slate-200 flex items-center justify-center gap-1"
          >
            <Eye className="w-3.5 h-3.5" />
            <span>Chi Tiáº¿t</span>
          </button>

          <Link
            href={`/category/${product.slug}`}
            className="py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-md flex items-center justify-center"
            title="Xem danh má»¥c"
          >
            <ShoppingCart className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
}
