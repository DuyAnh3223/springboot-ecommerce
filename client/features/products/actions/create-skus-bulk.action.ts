"use server";

import { createSkusBulk as createSkusBulkService } from "../services/product.service";

export async function createSkusBulkAction(productId: number, values: any[]) {
  try {
    const result = await createSkusBulkService(productId, values);
    return { success: true, skus: result };
  } catch (error: any) {
    console.error("Create SKUs bulk action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tạo hàng loạt biến thể thất bại.",
    };
  }
}
