"use client";

import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import { useRouter } from "next/navigation";
import {
  addCartItemAction,
  updateCartItemAction,
  removeCartItemAction,
  clearCartAction,
} from "../actions/cart.actions";
import { useCartStore } from "../stores/cart.store";

export function useCart() {
  const router = useRouter();
  const { isLoading, error, setError, run } = useAsyncAction();
  const store = useCartStore();

  const handleUpdateQuantity = async (skuId: number, quantity: number) => {
    if (quantity < 1) return;
    const previousSnapshot = store.getSnapshot();
    const previousSelected = store.selectedSkuIds;

    store.setPendingSku(skuId, true);
    store.updateQuantityOptimistic(skuId, quantity);

    const result = await run(() => updateCartItemAction(skuId, quantity));
    store.setPendingSku(skuId, false);

    if (result && result.success && result.data) {
      store.setCart(result.data);
    } else {
      store.restoreSnapshot(previousSnapshot, previousSelected);
      if (result?.requiresAuth) {
        router.push(`/sign-in?callbackUrl=${encodeURIComponent(window.location.pathname)}`);
      } else if (result?.error) {
        setError(result.error);
      }
    }
  };

  const handleRemoveItem = async (skuId: number) => {
    const previousSnapshot = store.getSnapshot();
    const previousSelected = store.selectedSkuIds;

    store.setPendingSku(skuId, true);
    store.removeItemOptimistic(skuId);

    const result = await run(() => removeCartItemAction(skuId));
    store.setPendingSku(skuId, false);

    if (result && result.success && result.data) {
      store.setCart(result.data);
    } else {
      store.restoreSnapshot(previousSnapshot, previousSelected);
      if (result?.requiresAuth) {
        router.push(`/sign-in?callbackUrl=${encodeURIComponent(window.location.pathname)}`);
      } else if (result?.error) {
        setError(result.error);
      }
    }
  };

  const handleClearCart = async () => {
    if (store.isClearPending) return;

    const previousSnapshot = store.getSnapshot();
    const previousSelected = store.selectedSkuIds;

    store.setIsClearPending(true);
    store.clear();

    const result = await run(() => clearCartAction());
    store.setIsClearPending(false);

    if (result && result.success && result.data) {
      store.setCart(result.data);
    } else {
      store.restoreSnapshot(previousSnapshot, previousSelected);
      if (result?.requiresAuth) {
        router.push(`/sign-in?callbackUrl=${encodeURIComponent(window.location.pathname)}`);
      } else if (result?.error) {
        setError(result.error);
      }
    }
  };

  const handleAddToCart = async (productSkuId: number, quantity: number = 1) => {
    const result = await run(() => addCartItemAction({ productSkuId, quantity }));

    if (result && result.success && result.data) {
      store.setCart(result.data);
      return true;
    } else {
      if (result?.requiresAuth) {
        router.push(`/sign-in?callbackUrl=${encodeURIComponent(window.location.pathname)}`);
      } else if (result?.error) {
        setError(result.error);
      }
      return false;
    }
  };

  return {
    isLoading,
    error,
    setError,
    handleUpdateQuantity,
    handleRemoveItem,
    handleClearCart,
    handleAddToCart,
  };
}
