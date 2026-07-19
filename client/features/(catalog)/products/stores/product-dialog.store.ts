import { create } from "zustand";
import { ProductResponse } from "../product.type";

type DialogTarget = "delete" | "publish" | "unpublish" | null;

interface ProductDialogState {
  open: DialogTarget;
  target: ProductResponse | null;
  openDialog: (type: Exclude<DialogTarget, null>, product: ProductResponse) => void;
  close: () => void;
}

export const useProductDialogStore = create<ProductDialogState>((set) => ({
  open: null,
  target: null,
  openDialog: (type, product) => set({ open: type, target: product }),
  close: () => set({ open: null, target: null }),
}));
