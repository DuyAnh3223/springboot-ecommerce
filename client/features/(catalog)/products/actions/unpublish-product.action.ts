"use server";

import { unpublishProduct as unpublishProductService } from "../services/product.service";

export async function unpublishProductAction(productId: number) {
  try {
    const result = await unpublishProductService(productId);
    return { success: true, product: result };
  } catch (error: any) {
    console.error("Unpublish product action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Hủy xuất bản sản phẩm thất bại.",
    };
  }
}
