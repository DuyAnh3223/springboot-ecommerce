"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { useCartStore } from "../stores/cart.store";
import { useCartHydration } from "./CartInitializer";
import { useCart } from "../hooks/useCart";
import { CartItemList } from "./CartItemList";
import { CartSummary } from "./CartSummary";
import { EmptyCart } from "./EmptyCart";
import { CartErrorState } from "./CartErrorState";
import { CartLoadingState } from "./CartLoadingState";
import { AlertCircle } from "lucide-react";

interface CartPageClientProps {
  initialError?: string | null;
}

export function CartPageClient({ initialError }: CartPageClientProps) {
  const router = useRouter();
  const { hydrationKey } = useCartHydration();
  const storeHydrationKey = useCartStore((state) => state.hydrationKey);
  const items = useCartStore((state) => state.items);
  const selectedSkuIds = useCartStore((state) => state.selectedSkuIds);
  const pendingSkuIds = useCartStore((state) => state.pendingSkuIds);
  const isClearPending = useCartStore((state) => state.isClearPending);
  const isHydrated = useCartStore((state) => state.isHydrated);
  const isAllSelected = useCartStore((state) => state.isAllSelected());
  const toggleItem = useCartStore((state) => state.toggleItem);
  const toggleAll = useCartStore((state) => state.toggleAll);
  const getSelectedQuantity = useCartStore((state) => state.getSelectedQuantity());
  const getSelectedSubtotal = useCartStore((state) => state.getSelectedSubtotal());

  const { error, setError, handleUpdateQuantity, handleRemoveItem, handleClearCart } = useCart();

  const handleRetry = () => {
    router.refresh();
  };

  if (!isHydrated || storeHydrationKey !== hydrationKey) {
    return <CartLoadingState />;
  }

  if (initialError) {
    return <CartErrorState message={initialError} onRetry={handleRetry} />;
  }

  if (items.length === 0) {
    return <EmptyCart />;
  }

  return (
    <div className="space-y-6">
      {error && (
        <div className="flex items-center justify-between rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 shadow-2xs">
          <div className="flex items-center space-x-2">
            <AlertCircle className="h-4 w-4 flex-shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
          <button
            onClick={() => setError(null)}
            className="text-xs font-semibold text-red-500 hover:text-red-700 underline"
          >
            Đóng
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
        <div className="lg:col-span-8">
          <CartItemList
            items={items}
            selectedSkuIds={selectedSkuIds}
            pendingSkuIds={pendingSkuIds}
            isClearPending={isClearPending}
            isAllSelected={isAllSelected}
            onToggleItem={toggleItem}
            onToggleAll={toggleAll}
            onUpdateQuantity={handleUpdateQuantity}
            onRemoveItem={handleRemoveItem}
            onClearCart={handleClearCart}
          />
        </div>

        <div className="lg:col-span-4">
          <div className="lg:sticky lg:top-24">
            <CartSummary
              selectedCount={selectedSkuIds.length}
              selectedQuantity={getSelectedQuantity}
              subtotal={getSelectedSubtotal}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
