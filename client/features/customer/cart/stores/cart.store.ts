import { create } from "zustand";
import { CartItem, CartSnapshot } from "../types/cart.types";

interface CartState {
  cartId: number | null;
  status: string;
  userId: string | null;
  items: CartItem[];
  selectedSkuIds: number[];
  isHydrated: boolean;
  hydrationKey: string | null;
  pendingSkuIds: number[];
  isClearPending: boolean;
  isGuest: boolean;

  // Actions
  setCart: (snapshot: CartSnapshot, hydrationKey?: string) => void;
  reset: (hydrationKey?: string) => void;
  restoreSnapshot: (snapshot: CartSnapshot, previousSelectedSkuIds?: number[]) => void;
  toggleItem: (skuId: number) => void;
  toggleAll: (selected: boolean) => void;
  updateQuantityOptimistic: (skuId: number, quantity: number) => void;
  removeItemOptimistic: (skuId: number) => void;
  setPendingSku: (skuId: number, pending: boolean) => void;
  setIsClearPending: (pending: boolean) => void;
  clear: () => void;

  // Selectors
  getTotalQuantity: () => number;
  getSelectedQuantity: () => number;
  getSelectedSubtotal: () => number;
  isAllSelected: () => boolean;
  getSnapshot: () => CartSnapshot;
}

export const useCartStore = create<CartState>((set, get) => ({
  cartId: null,
  status: "ACTIVE",
  userId: null,
  items: [],
  selectedSkuIds: [],
  isHydrated: false,
  hydrationKey: null,
  pendingSkuIds: [],
  isClearPending: false,
  isGuest: false,

  setCart: (snapshot, hydrationKey) => {
    const newItems = snapshot.items || [];
    set((state) => {
      const currentSelected = state.selectedSkuIds;
      const newSelectedSkuIds = state.isHydrated
        ? newItems
            .map((item) => item.productSkuId)
            .filter((skuId) => currentSelected.includes(skuId) || !state.items.some((i) => i.productSkuId === skuId))
        : newItems.map((item) => item.productSkuId);

      return {
        cartId: snapshot.cartId,
        status: snapshot.status || "ACTIVE",
        userId: snapshot.userId,
        items: newItems,
        selectedSkuIds: newSelectedSkuIds,
        isHydrated: true,
        hydrationKey: hydrationKey ?? state.hydrationKey,
        isGuest: snapshot.isGuest ?? false,
      };
    });
  },

  reset: (hydrationKey) => {
    set({
      cartId: null,
      status: "ACTIVE",
      userId: null,
      items: [],
      selectedSkuIds: [],
      isHydrated: true,
      hydrationKey: hydrationKey ?? null,
      pendingSkuIds: [],
      isClearPending: false,
      isGuest: false,
    });
  },

  restoreSnapshot: (snapshot, previousSelectedSkuIds) => {
    const newItems = snapshot.items || [];
    const targetSelected = previousSelectedSkuIds ?? get().selectedSkuIds;
    const restoredSelected = newItems
      .map((item) => item.productSkuId)
      .filter((skuId) => targetSelected.includes(skuId));

    set({
      cartId: snapshot.cartId,
      status: snapshot.status || "ACTIVE",
      userId: snapshot.userId,
      items: newItems,
      selectedSkuIds: restoredSelected,
      isHydrated: true,
      isGuest: snapshot.isGuest ?? false,
    });
  },

  toggleItem: (skuId) => {
    set((state) => {
      const exists = state.selectedSkuIds.includes(skuId);
      const newSelected = exists
        ? state.selectedSkuIds.filter((id) => id !== skuId)
        : [...state.selectedSkuIds, skuId];
      return { selectedSkuIds: newSelected };
    });
  },

  toggleAll: (selected) => {
    set((state) => ({
      selectedSkuIds: selected ? state.items.map((i) => i.productSkuId) : [],
    }));
  },

  updateQuantityOptimistic: (skuId, quantity) => {
    set((state) => ({
      items: state.items.map((item) =>
        item.productSkuId === skuId ? { ...item, quantity } : item
      ),
    }));
  },

  removeItemOptimistic: (skuId) => {
    set((state) => ({
      items: state.items.filter((item) => item.productSkuId !== skuId),
      selectedSkuIds: state.selectedSkuIds.filter((id) => id !== skuId),
    }));
  },

  setPendingSku: (skuId, pending) => {
    set((state) => ({
      pendingSkuIds: pending
        ? [...state.pendingSkuIds, skuId]
        : state.pendingSkuIds.filter((id) => id !== skuId),
    }));
  },

  setIsClearPending: (pending) => {
    set({ isClearPending: pending });
  },

  clear: () => {
    set((state) => ({
      items: [],
      selectedSkuIds: [],
      isHydrated: true,
      pendingSkuIds: [],
      isClearPending: state.isClearPending,
    }));
  },

  getTotalQuantity: () => {
    return get().items.reduce((total, item) => total + item.quantity, 0);
  },

  getSelectedQuantity: () => {
    const { items, selectedSkuIds } = get();
    return items
      .filter((item) => selectedSkuIds.includes(item.productSkuId))
      .reduce((total, item) => total + item.quantity, 0);
  },

  getSelectedSubtotal: () => {
    const { items, selectedSkuIds } = get();
    return items
      .filter((item) => selectedSkuIds.includes(item.productSkuId))
      .reduce((sum, item) => sum + item.unitPrice * item.quantity, 0);
  },

  isAllSelected: () => {
    const { items, selectedSkuIds } = get();
    if (items.length === 0) return false;
    return items.every((item) => selectedSkuIds.includes(item.productSkuId));
  },

  getSnapshot: () => {
    const { cartId, status, items, userId, isGuest } = get();
    return { cartId, status, items, userId, isGuest };
  },
}));
