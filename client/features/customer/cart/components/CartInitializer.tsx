"use client";

import { createContext, useContext, useEffect, useRef, useState, useSyncExternalStore } from "react";
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
  guestMergeStatus: "unknown" | "idle" | "pending" | "succeeded" | "failed";
}

const CartHydrationContext = createContext<CartHydrationContextValue>({
  hydrationKey: "",
  guestMergeStatus: "unknown",
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
  const { hydrationKey, guestMergeStatus } = useContext(CartHydrationContext);
  const isStoreHydrated = useCartStore((state) => state.hydrationKey === hydrationKey);
  const isClient = useSyncExternalStore(
    () => () => undefined,
    () => true,
    () => false,
  );

  return {
    hydrationKey,
    isHydrated: isClient && isStoreHydrated,
    guestMergeStatus,
  };
}

export function CartInitializer({ cart, scopeKey, children }: CartInitializerProps) {
  const hydrationKey = getCartHydrationKey(scopeKey, cart);
  const mergeAttemptRef = useRef<string | null>(null);
  const [guestMergeStatus, setGuestMergeStatus] = useState<
    "unknown" | "idle" | "pending" | "succeeded" | "failed"
  >("unknown");

  useEffect(() => {
    let isMounted = true;
    const scheduleGuestMergeStatus = (
      status: "idle" | "pending" | "succeeded" | "failed",
    ) => {
      queueMicrotask(() => {
        if (isMounted) setGuestMergeStatus(status);
      });
    };

    if (cart) {
      useCartStore.getState().setCart(cart, hydrationKey);

      const guestItems = getGuestCart();
      if (guestItems.length === 0) {
        scheduleGuestMergeStatus("idle");
      } else {
        scheduleGuestMergeStatus("pending");
        const mergeId = getOrCreateGuestCartMergeId();
        if (mergeAttemptRef.current === mergeId) {
          return () => {
            isMounted = false;
          };
        }
        mergeAttemptRef.current = mergeId;

        mergeGuestCartAction(
          mergeId,
          guestItems.map((item) => ({ skuId: item.productSkuId, quantity: item.quantity })),
        ).then((result) => {
          if (!isMounted) return;
          if (!result.success || !result.data) {
            scheduleGuestMergeStatus("failed");
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
          scheduleGuestMergeStatus("succeeded");
          useCartStore.getState().setGuestMergeNotices(applied.notices);
          useCartStore.getState().setCart(result.data.cart, hydrationKey);
          if (applied.allResultsApplied) {
            clearGuestCartMergeId();
            if (applied.retainedItems.length === 0) {
              clearGuestCart();
            }
          }
        }).catch(() => {
          if (!isMounted) return;
          scheduleGuestMergeStatus("failed");
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
      scheduleGuestMergeStatus("idle");
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

    return () => {
      isMounted = false;
    };
  }, [cart, hydrationKey]);

  return (
    <CartHydrationContext.Provider value={{ hydrationKey, guestMergeStatus }}>
      {children}
    </CartHydrationContext.Provider>
  );
}
