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
import Link from "next/link";
import { AlertCircle, Info } from "lucide-react";

interface CartPageClientProps {
  initialError?: string | null;
  isGuest?: boolean;
}

export function CartPageClient({ initialError, isGuest: propIsGuest }: CartPageClientProps) {
  const router = useRouter();
  const { hydrationKey } = useCartHydration();
  const storeHydrationKey = useCartStore((state) => state.hydrationKey);
  const storeIsGuest = useCartStore((state) => state.isGuest);
  const items = useCartStore((state) => state.items);
  const selectedSkuIds = useCartStore((state) => state.selectedSkuIds);
  const pendingSkuIds = useCartStore((state) => state.pendingSkuIds);
  const isClearPending = useCartStore((state) => state.isClearPending);
  const isHydrated = useCartStore((state) => state.isHydrated);
  const guestMergeNotices = useCartStore((state) => state.guestMergeNotices);
  const isAllSelected = useCartStore((state) => state.isAllSelected());
  const toggleItem = useCartStore((state) => state.toggleItem);
  const toggleAll = useCartStore((state) => state.toggleAll);
  const getSelectedQuantity = useCartStore((state) => state.getSelectedQuantity());
  const getSelectedSubtotal = useCartStore((state) => state.getSelectedSubtotal());

  const { error, setError, handleUpdateQuantity, handleRemoveItem, handleClearCart } = useCart();
  const isGuest = propIsGuest ?? storeIsGuest;

  const mergeNotice = guestMergeNotices.length > 0 && (
    <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 shadow-2xs">
      <p className="font-semibold">Một số sản phẩm chưa được đồng bộ</p>
      <ul className="mt-2 list-disc space-y-1 pl-5">
        {guestMergeNotices.map((notice, index) => (
          <li key={`${notice.skuId}-${index}`}>
            {notice.skuId > 0
              ? `SKU ${notice.skuId} (${notice.quantity} sản phẩm): ${notice.message}`
              : notice.message}
          </li>
        ))}
      </ul>
    </div>
  );

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
    return (
      <div className="space-y-4">
        {mergeNotice}
        {isGuest && (
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50/90 p-4 text-sm text-amber-900 shadow-2xs">
            <div className="flex items-start space-x-3">
              <Info className="h-5 w-5 flex-shrink-0 text-amber-600 mt-0.5" />
              <div>
                <p className="font-bold text-amber-950">Bạn chưa đăng nhập</p>
                <p className="mt-0.5 text-xs sm:text-sm text-amber-800">
                  Đăng nhập để xem giỏ hàng đã lưu của bạn hoặc tiếp tục mua sắm để thêm sản phẩm.
                </p>
              </div>
            </div>
            <Link
              href="/sign-in?callbackUrl=/cart"
              className="inline-flex items-center justify-center rounded-lg bg-amber-600 px-3.5 py-1.5 text-xs font-bold text-white hover:bg-amber-700 transition-colors shadow-2xs shrink-0"
            >
              Đăng nhập ngay
            </Link>
          </div>
        )}
        <EmptyCart />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {mergeNotice}
      {isGuest && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50/90 p-4 text-sm text-amber-900 shadow-2xs">
          <div className="flex items-start space-x-3">
            <Info className="h-5 w-5 flex-shrink-0 text-amber-600 mt-0.5" />
            <div>
              <p className="font-bold text-amber-950">Bạn đang xem giỏ hàng tạm thời</p>
              <p className="mt-0.5 text-xs sm:text-sm text-amber-800">
                Giá sản phẩm có thể thay đổi và giỏ hàng sẽ được tự động lưu vào tài khoản khi bạn đăng nhập.
              </p>
            </div>
          </div>
          <Link
            href="/sign-in?callbackUrl=/cart"
            className="inline-flex items-center justify-center rounded-lg bg-amber-600 px-3.5 py-1.5 text-xs font-bold text-white hover:bg-amber-700 transition-colors shadow-2xs shrink-0"
          >
            Đăng nhập để lưu
          </Link>
        </div>
      )}

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
              selectedSkuIds={selectedSkuIds}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
