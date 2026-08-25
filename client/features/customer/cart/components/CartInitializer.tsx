"use client";

import { createContext, useContext, useEffect, useRef, useSyncExternalStore } from "react";
import { CartSnapshot } from "../types/cart.types";
import { useCartStore } from "../stores/cart.store";
import {
  clearGuestCart,
  clearGuestCartMergeId,
  getGuestCart,
  getOrCreateGuestCartMergeId,
  saveGuestCart,
} from "../utils/guest-cart.utils";
import { mergeGuestCartAction } from "../actions/merge-guest-cart.actions";
import { applyGuestMergeResult } from "../utils/merge-guest-cart";

interface CartHydrationContextValue {
  hydrationKey: string;
}

const CartHydrationContext = createContext<CartHydrationContextValue>({
  hydrationKey: "",
});

interface CartInitializerProps {
  cart: CartSnapshot | null;
  scopeKey: string;
  children: React.ReactNode;
}

export function getCartHydrationKey(scopeKey: string, cart: CartSnapshot | null): string {
  return JSON.stringify({
    scopeKey,
    cartId: cart?.cartId ?? null,
    userId: cart?.userId ?? null,
    items: cart?.items.map((item) => [item.productSkuId, item.quantity, item.unitPrice]) ?? [],
  });
}

export function useCartHydration() {
  const { hydrationKey } = useContext(CartHydrationContext);
  const isStoreHydrated = useCartStore((state) => state.hydrationKey === hydrationKey);
  const isClient = useSyncExternalStore(
    () => () => undefined,
    () => true,
    () => false,
  );

  return { hydrationKey, isHydrated: isClient && isStoreHydrated };
}

export function CartInitializer({ cart, scopeKey, children }: CartInitializerProps) {
  const hydrationKey = getCartHydrationKey(scopeKey, cart);
  const mergeAttemptRef = useRef<string | null>(null);

  useEffect(() => {
    if (cart) {
      useCartStore.getState().setCart(cart, hydrationKey);

      const guestItems = getGuestCart();
      if (guestItems.length > 0) {
        const mergeId = getOrCreateGuestCartMergeId();
        if (mergeAttemptRef.current === mergeId) return;
        mergeAttemptRef.current = mergeId;

        mergeGuestCartAction(
          mergeId,
          guestItems.map((item) => ({ skuId: item.productSkuId, quantity: item.quantity })),
        ).then((result) => {
          if (!result.success || !result.data) {
            useCartStore.getState().setGuestMergeNotices([
              {
                skuId: 0,
                quantity: 0,
                reasonCode: null,
                message: result.error || "Không thể đồng bộ giỏ hàng. Vui lòng thử lại sau.",
              },
            ]);
            return;
          }

          const applied = applyGuestMergeResult(guestItems, result.data.merge.items);
          saveGuestCart(applied.retainedItems);
          useCartStore.getState().setGuestMergeNotices(applied.notices);
          useCartStore.getState().setCart(result.data.cart, hydrationKey);
          if (applied.allResultsApplied) {
            clearGuestCartMergeId();
            if (applied.retainedItems.length === 0) {
              clearGuestCart();
            }
          }
        }).catch(() => {
          useCartStore.getState().setGuestMergeNotices([
            {
              skuId: 0,
              quantity: 0,
              reasonCode: null,
              message: "Không thể đồng bộ giỏ hàng. Sản phẩm tạm thời vẫn được giữ lại.",
            },
          ]);
        });
      }
    } else {
      const guestItems = getGuestCart();
      const guestSnapshot: CartSnapshot = {
        cartId: null,
        status: "ACTIVE",
        items: guestItems,
        userId: null,
        isGuest: true,
      };
      useCartStore.getState().setCart(guestSnapshot, hydrationKey);
    }
  }, [cart, hydrationKey]);

  return (
    <CartHydrationContext.Provider value={{ hydrationKey }}>
      {children}
    </CartHydrationContext.Provider>
  );
}
