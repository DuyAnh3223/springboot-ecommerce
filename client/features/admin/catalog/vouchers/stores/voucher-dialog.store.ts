import { create } from "zustand";
import { VoucherResponse } from "@/features/vouchers/voucher.type";

type DialogTarget = "create" | "edit" | "toggle-active" | null;

interface VoucherDialogState {
  open: DialogTarget;
  target: VoucherResponse | null;
  openDialog: (type: Exclude<DialogTarget, null>, voucher?: VoucherResponse | null) => void;
  close: () => void;
}

export const useVoucherDialogStore = create<VoucherDialogState>((set) => ({
  open: null,
  target: null,
  openDialog: (type, voucher = null) => set({ open: type, target: voucher }),
  close: () => set({ open: null, target: null }),
}));
