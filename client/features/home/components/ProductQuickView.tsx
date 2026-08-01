"use client";

import { useEffect } from "react";
import { CatalogProductItem } from "@/features/(catalog)/catalog/types/catalog.types";
import { getSafeImageUrl } from "@/lib/utils";
import { X, CheckCircle2, XCircle, Shield, Truck } from "lucide-react";
import Link from "next/link";

interface ProductQuickViewProps {
  product: CatalogProductItem | null;
  onClose: () => void;
}

export default function ProductQuickView({ product, onClose }: ProductQuickViewProps) {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    if (product) {
      document.body.style.overflow = "hidden";
      window.addEventListener("keydown", handleKeyDown);
    }
    return () => {
      document.body.style.overflow = "auto";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [product, onClose]);

  if (!product) return null;

  const safeImg = getSafeImageUrl(product.thumbnail || product.primaryImageUrl);

  const formatVND = (price: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

  const isOutOfStock = product.totalStock <= 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div
        className="relative w-full max-w-2xl bg-white border border-slate-200 rounded-3xl p-6 shadow-2xl text-slate-900 overflow-hidden max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close Button */}
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng cửa sổ"
          className="absolute top-4 right-4 p-2 rounded-xl bg-slate-100 text-slate-500 hover:text-white hover:bg-rose-600 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 items-start">
          {/* Image */}
          <div className="aspect-square rounded-2xl bg-slate-50 border border-slate-200 overflow-hidden flex items-center justify-center p-2">
            {safeImg ? (
              <img src={safeImg} alt={product.name} className="w-full h-full object-contain" />
            ) : (
              <span className="text-slate-400 text-sm">ABTechZone</span>
            )}
          </div>

          {/* Details */}
          <div className="space-y-4">
            <div>
              {product.brand?.name && (
                <span className="text-xs font-bold text-rose-600 uppercase tracking-wider block mb-1">
                  {product.brand.name}
                </span>
              )}
              <h2 className="text-lg font-bold text-slate-900 leading-snug">{product.name}</h2>
            </div>

            {/* Price */}
            <div className="p-3.5 rounded-2xl bg-rose-50/60 border border-rose-100">
              <span className="text-xs text-slate-500 font-medium block mb-0.5">Giá sản phẩm:</span>
              <span className="text-xl font-extrabold text-rose-600">
                {formatVND(product.priceMin)}
                {product.priceMax > product.priceMin && ` - ${formatVND(product.priceMax)}`}
              </span>
            </div>

            {/* Stock status */}
            <div className="flex items-center gap-2 text-xs font-semibold">
              {isOutOfStock ? (
                <span className="inline-flex items-center gap-1 text-rose-400">
                  <XCircle className="w-4 h-4" /> Tạm hết hàng
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 text-emerald-400">
                  <CheckCircle2 className="w-4 h-4" /> Còn hàng (Sẵn sàng giao)
                </span>
              )}
            </div>

            {/* Features summary */}
            <div className="space-y-2 text-xs text-slate-300 border-t border-b border-slate-800 py-3">
              <div className="flex items-center gap-2">
                <Shield className="w-4 h-4 text-emerald-400" />
                <span>Bảo hành 1 đổi 1 chính hãng tại ABTechZone</span>
              </div>
              <div className="flex items-center gap-2">
                <Truck className="w-4 h-4 text-indigo-400" />
                <span>Miễn phí giao hàng toàn quốc đơn từ 2.000.000đ</span>
              </div>
            </div>

            {/* CTA */}
            <div className="pt-2 flex items-center gap-3">
              <Link
                href={`/category/${product.slug}`}
                onClick={onClose}
                className="flex-1 py-3 text-center rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs shadow-lg transition-all"
              >
                Xem Chi Tiết & Mua Hàng
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
