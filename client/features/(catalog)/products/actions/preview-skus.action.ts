"use server";

import { previewSkus as previewSkusService } from "../services/product.service";

export async function previewSkusAction(productId: number, values: { attributes: Record<string, any[]> }) {
  try {
    const result = await previewSkusService(productId, values);
    return { success: true, combinations: result };
  } catch (error: any) {
    console.error("Preview SKUs action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Xem trước biến thể thất bại.",
    };
  }
}
