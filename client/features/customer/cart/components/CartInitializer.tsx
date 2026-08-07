"use client";

import { createContext, useContext, useEffect, useSyncExternalStore } from "react";
import { CartSnapshot } from "../types/cart.types";
import { useCartStore } from "../stores/cart.store";

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
    } else {
      useCartStore.getState().reset(hydrationKey);
    }
  }, [cart, hydrationKey]);

  return (
    <CartHydrationContext.Provider value={{ hydrationKey }}>
      {children}
    </CartHydrationContext.Provider>
  );
}
