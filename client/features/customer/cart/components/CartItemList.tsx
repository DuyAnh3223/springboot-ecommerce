"use client";

import React from "react";
import { CartItem } from "../types/cart.types";
import { CartSelectionBar } from "./CartSelectionBar";
import { CartItemRow } from "./CartItemRow";

interface CartItemListProps {
  items: CartItem[];
  selectedSkuIds: number[];
  pendingSkuIds: number[];
  isClearPending?: boolean;
  isAllSelected: boolean;
  onToggleItem: (skuId: number) => void;
  onToggleAll: (selected: boolean) => void;
  onUpdateQuantity: (skuId: number, qty: number) => void;
  onRemoveItem: (skuId: number) => void;
  onClearCart: () => void;
}

export function CartItemList({
  items,
  selectedSkuIds,
  pendingSkuIds,
  isClearPending = false,
  isAllSelected,
  onToggleItem,
  onToggleAll,
  onUpdateQuantity,
  onRemoveItem,
  onClearCart,
}: CartItemListProps) {
  const totalCount = items.length;

  return (
    <div className="space-y-4">
      <CartSelectionBar
        totalCount={totalCount}
        isAllSelected={isAllSelected}
        onToggleAll={onToggleAll}
        onClearCart={onClearCart}
        isPending={pendingSkuIds.length > 0}
        isClearPending={isClearPending}
      />

      <div className="space-y-3">
        {items.map((item) => (
          <CartItemRow
            key={item.productSkuId}
            item={item}
            isSelected={selectedSkuIds.includes(item.productSkuId)}
            isPending={pendingSkuIds.includes(item.productSkuId) || isClearPending}
            onToggleSelect={onToggleItem}
            onUpdateQuantity={onUpdateQuantity}
            onRemove={onRemoveItem}
          />
        ))}
      </div>
    </div>
  );
}
