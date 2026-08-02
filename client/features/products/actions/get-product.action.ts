"use server";

import { getProduct as getProductService } from "../services/product.service";

export async function getProductAction(productId: number) {
  try {
    const result = await getProductService(productId);
    return { success: true, product: result };
  } catch (error: any) {
    console.error("Get product action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Lấy thông tin chi tiết sản phẩm thất bại.",
    };
  }
}
