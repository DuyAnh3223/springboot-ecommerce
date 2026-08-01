"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ShoppingCart, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/axios";
import { CatalogProductItem } from "../types/catalog.types";

interface AddToCartButtonProps {
  product: CatalogProductItem;
}

export function AddToCartButton({ product }: AddToCartButtonProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const isOutOfStock = product.totalStock <= 0;
  const isMultiVariant = product.activeSkuCount > 1;

  const handleAction = async () => {
    if (isOutOfStock) return;

    if (isMultiVariant) {
      router.push(`/products/${product.slug}`);
      return;
    }

    try {
      setLoading(true);
      // Calls backend Cart API directly
      await api.post("/cart/add", {
        productSkuId: product.id, // For single SKU product, ID maps directly
        quantity: 1,
      });
      router.refresh();
    } catch (err: any) {
      if (err.response?.status === 401) {
        router.push(`/auth/sign-in?callbackUrl=${encodeURIComponent(window.location.href)}`);
      } else {
        // Fallback navigate to product details
        router.push(`/products/${product.slug}`);
      }
    } finally {
      setLoading(false);
    }
  };

  if (isOutOfStock) {
    return (
      <Button disabled variant="outline" size="sm" className="h-6 text-[10px] px-2 opacity-50 border-slate-200 text-slate-400">
        Hết hàng
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
      >
        Tùy chọn <ArrowRight className="w-2.5 h-2.5 ml-0.5" />
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
