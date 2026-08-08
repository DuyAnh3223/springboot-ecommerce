"use client";

import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/features/auth/stores/auth.store";
import {
  addCartItemAction,
  updateCartItemAction,
  removeCartItemAction,
  clearCartAction,
} from "../actions/cart.actions";
import { useCartStore } from "../stores/cart.store";
import {
  addGuestCartItem,
  updateGuestCartItemQuantity,
  removeGuestCartItem,
  clearGuestCart,
} from "../utils/guest-cart.utils";

export function useCart() {
  const router = useRouter();
  const { isLoading, error, setError, run } = useAsyncAction();
  const store = useCartStore();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  const isGuestMode = !isAuthenticated || store.isGuest;

  const handleUpdateQuantity = async (skuId: number, quantity: number) => {
    if (quantity < 1) return;

    if (isGuestMode) {
      store.setPendingSku(skuId, true);
      const updatedItems = updateGuestCartItemQuantity(skuId, quantity);
      store.setCart({
        cartId: null,
        status: "ACTIVE",
        items: updatedItems,
        userId: null,
        isGuest: true,
      });
      store.setPendingSku(skuId, false);
      return;
    }

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
    if (isGuestMode) {
      store.setPendingSku(skuId, true);
      const updatedItems = removeGuestCartItem(skuId);
      store.setCart({
        cartId: null,
        status: "ACTIVE",
        items: updatedItems,
        userId: null,
        isGuest: true,
      });
      store.setPendingSku(skuId, false);
      return;
    }

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

    if (isGuestMode) {
      store.setIsClearPending(true);
      clearGuestCart();
      store.clear();
      store.setCart({
        cartId: null,
        status: "ACTIVE",
        items: [],
        userId: null,
        isGuest: true,
      });
      store.setIsClearPending(false);
      return;
    }

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

  const handleAddToCart = async (
    productSkuId: number,
    quantity: number = 1,
    itemDetails?: {
      productName?: string;
      imageUrl?: string;
      unitPrice?: number;
      skuCode?: string;
    }
  ) => {
    if (isGuestMode) {
      const guestItem = {
        productSkuId,
        skuCode: itemDetails?.skuCode || "",
        productName: itemDetails?.productName || "Sản phẩm",
        imageUrl: itemDetails?.imageUrl || "",
        quantity,
        unitPrice: itemDetails?.unitPrice || 0,
      };

      const result = addGuestCartItem(guestItem);
      if (!result.success) {
        setError(result.error || "Không thể thêm vào giỏ hàng.");
        return false;
      }

      store.setCart({
        cartId: null,
        status: "ACTIVE",
        items: result.items,
        userId: null,
        isGuest: true,
      });
      return true;
    }

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
