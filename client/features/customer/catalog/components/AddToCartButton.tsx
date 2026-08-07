"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { ShoppingCart, ArrowRight, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CatalogProductItem } from "@/features/customer/catalog/types/catalog.types";
import { useCart } from "@/features/customer/cart/hooks/useCart";

interface AddToCartButtonProps {
  product: CatalogProductItem;
  className?: string;
  iconOnly?: boolean;
}

export function AddToCartButton({ product, className, iconOnly = false }: AddToCartButtonProps) {
  const router = useRouter();
  const { handleAddToCart } = useCart();
  const [loading, setLoading] = useState(false);

  const isOutOfStock = product.totalStock <= 0;
  const isMultiVariant = product.activeSkuCount > 1;
  const isSkuMappingMissing = !isMultiVariant && !product.singleSkuId;

  const handleAction = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (isOutOfStock) return;

    if (isMultiVariant) {
      router.push(`/products/${product.slug}`);
      return;
    }

    if (!product.singleSkuId) return;

    setLoading(true);
    try {
      await handleAddToCart(product.singleSkuId, 1);
    } finally {
      setLoading(false);
    }
  };

  if (iconOnly) {
    if (isOutOfStock) {
      return (
        <Button
          disabled
          size="sm"
          className={className || "py-2 px-3 h-8 rounded-xl bg-slate-200 text-slate-400 cursor-not-allowed"}
          title="Hết hàng"
        >
          <ShoppingCart className="w-3.5 h-3.5" />
        </Button>
      );
    }

    if (isMultiVariant) {
      return (
        <Button
          type="button"
          onClick={handleAction}
          size="sm"
          className={className || "py-2 px-3 h-8 rounded-xl bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold transition-all shadow-md flex items-center justify-center"}
          title="Xem tùy chọn sản phẩm"
        >
          <ArrowRight className="w-3.5 h-3.5" />
        </Button>
      );
    }

    if (isSkuMappingMissing) {
      return (
        <Button
          disabled
          size="sm"
          className={className || "py-2 px-3 h-8 rounded-xl bg-slate-200 text-slate-400 cursor-not-allowed"}
          title="Sản phẩm chưa sẵn sàng để thêm vào giỏ hàng"
        >
          <ShoppingCart className="w-3.5 h-3.5" />
        </Button>
      );
    }

    return (
      <Button
        type="button"
        onClick={handleAction}
        disabled={loading}
        size="sm"
        className={className || "py-2 px-3 h-8 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition-all shadow-md flex items-center justify-center"}
        title="Thêm vào giỏ hàng"
      >
        {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <ShoppingCart className="w-3.5 h-3.5" />}
      </Button>
    );
  }

  if (isOutOfStock) {
    return (
      <Button disabled variant="outline" size="sm" className="h-6 text-[10px] px-2 opacity-50 border-slate-200 text-slate-400">
        Hết Hàng
      </Button>
    );
  }

  if (isMultiVariant) {
    return (
      <Button
        onClick={handleAction}
        variant="outline"
        size="sm"
        className="h-6 text-[10px] px-2 border-slate-300 text-slate-700 hover:text-shop_light_green hover:bg-emerald-50/60 font-semibold"
        title="Tính năng chọn tùy chọn biến thể sẽ được hỗ trợ ở trang chi tiết sản phẩm"
      >
        Tùy chọn <ArrowRight className="w-2.5 h-2.5 ml-0.5" />
      </Button>
    );
  }

  if (isSkuMappingMissing) {
    return (
      <Button
        disabled
        variant="outline"
        size="sm"
        className="h-6 text-[10px] px-2 border-slate-300 text-slate-400 font-semibold cursor-not-allowed"
        title="Sản phẩm chưa có mã SKU hợp lệ để thêm vào giỏ hàng"
      >
        Tạm thời không khả dụng
      </Button>
    );
  }

  return (
    <Button
      onClick={handleAction}
      disabled={loading}
      size="sm"
      className="h-6 text-[10px] px-2.5 bg-shop_light_green hover:bg-shop_dark_green text-white font-bold shadow-2xs transition-all"
    >
      <ShoppingCart className="w-2.5 h-2.5 mr-0.5" />
      {loading ? "..." : "Thêm"}
    </Button>
  );
}
