"use client";

import React, { useState } from "react";
import Image from "next/image";
import { Trash2 } from "lucide-react";
import { formatCurrency } from "@/shared/utils";
import { Button } from "@/components/ui/button";
import { CartItem } from "../types/cart.types";
import { QuantityPicker } from "./QuantityPicker";

interface CartItemRowProps {
  item: CartItem;
  isSelected: boolean;
  isPending: boolean;
  onToggleSelect: (skuId: number) => void;
  onUpdateQuantity: (skuId: number, qty: number) => void;
  onRemove: (skuId: number) => void;
}

export function CartItemRow({
  item,
  isSelected,
  isPending,
  onToggleSelect,
  onUpdateQuantity,
  onRemove,
}: CartItemRowProps) {
  const [imgError, setImgError] = useState(false);
  const lineTotal = item.unitPrice * item.quantity;
  const imageSrc = !imgError && item.imageUrl ? item.imageUrl : "/images/placeholder.png";

  return (
    <div
      className={`group flex flex-col gap-4 rounded-xl border p-4 transition-all sm:flex-row sm:items-center sm:justify-between ${
        isSelected ? "border-emerald-200 bg-emerald-50/20" : "border-slate-100 bg-white"
      } ${isPending ? "opacity-60 pointer-events-none" : ""}`}
    >
      {/* Left: Checkbox & Product info */}
      <div className="flex items-center space-x-3 sm:space-x-4">
        <input
          type="checkbox"
          checked={isSelected}
          onChange={() => onToggleSelect(item.productSkuId)}
          className="h-4 w-4 rounded border-slate-300 text-shop_light_green focus:ring-shop_light_green cursor-pointer"
          aria-label={`Chọn sản phẩm ${item.productName}`}
        />

        <div className="relative h-16 w-16 flex-shrink-0 overflow-hidden rounded-lg border border-slate-100 bg-slate-50 sm:h-20 sm:w-20">
          <Image
            src={imageSrc}
            alt={item.productName}
            fill
            sizes="80px"
            className="object-contain p-1"
            onError={() => setImgError(true)}
          />
        </div>

        <div className="flex flex-col">
          <h3 className="line-clamp-2 text-sm font-semibold text-slate-800 sm:text-base">
            {item.productName}
          </h3>
          {item.skuCode && (
            <span className="mt-0.5 text-xs text-slate-400 font-mono">
              SKU: {item.skuCode}
            </span>
          )}
          <span className="mt-1 text-sm font-bold text-shop_dark_green sm:hidden">
            {formatCurrency(item.unitPrice)}
          </span>
        </div>
      </div>

      {/* Right: Unit price, Quantity, Line Total & Action */}
      <div className="flex items-center justify-between sm:space-x-6 sm:justify-end">
        <div className="hidden text-right sm:block">
          <span className="text-sm font-semibold text-slate-700">
            {formatCurrency(item.unitPrice)}
          </span>
        </div>

        <QuantityPicker
          quantity={item.quantity}
          onUpdate={(newQty) => onUpdateQuantity(item.productSkuId, newQty)}
          disabled={isPending}
          productName={item.productName}
        />

        <div className="text-right min-w-[90px]">
          <span className="text-sm font-bold text-slate-900 sm:text-base">
            {formatCurrency(lineTotal)}
          </span>
        </div>

        <Button
          type="button"
          variant="ghost"
          size="icon"
          disabled={isPending}
          onClick={() => onRemove(item.productSkuId)}
          className="h-8 w-8 text-slate-400 hover:bg-red-50 hover:text-red-600 rounded-lg transition-colors"
          aria-label={`Xóa sản phẩm ${item.productName} khỏi giỏ hàng`}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
