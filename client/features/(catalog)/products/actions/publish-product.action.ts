"use server";

import { publishProduct as publishProductService } from "../services/product.service";

export async function publishProductAction(productId: number) {
  try {
    const result = await publishProductService(productId);
    return { success: true, product: result };
  } catch (error: any) {
    console.error("Publish product action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Xuất bản sản phẩm thất bại.",
    };
  }
}
