"use client";

import { createContext, useContext, useEffect, useSyncExternalStore } from "react";
import { CartSnapshot } from "../types/cart.types";
import { useCartStore } from "../stores/cart.store";
import { getGuestCart, clearGuestCart } from "../utils/guest-cart.utils";
import { mergeGuestCartAction } from "../actions/merge-guest-cart.actions";

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

  useEffect(() => {
    if (cart) {
      useCartStore.getState().setCart(cart, hydrationKey);

      const guestItems = getGuestCart();
      if (guestItems.length > 0) {
        mergeGuestCartAction(
          guestItems.map((i) => ({ productSkuId: i.productSkuId, quantity: i.quantity }))
        )
          .then((res) => {
            if (res.success && res.data) {
              useCartStore.getState().setCart(res.data, hydrationKey);
            }
            clearGuestCart();
          })
          .catch(() => {
            clearGuestCart();
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
