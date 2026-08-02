"use server";

import { reconcileSkus as reconcileSkusService } from "../services/product.service";
import { ProductReconcilePayload } from "../types/sku.draft.type";

export async function reconcileSkusAction(productId: number, payload: ProductReconcilePayload) {
  try {
    const product = await reconcileSkusService(productId, payload);
    return { success: true, product };
  } catch (error: any) {
    console.error("Reconcile SKUs action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Lưu danh sách biến thể SKU thất bại.",
    };
  }
}
