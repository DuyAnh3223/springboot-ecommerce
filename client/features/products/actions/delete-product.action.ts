"use server";

import { deleteProduct as deleteProductService } from "../services/product.service";

export async function deleteProductAction(productId: number) {
  try {
    await deleteProductService(productId);
    return { success: true };
  } catch (error: any) {
    console.error("Delete product action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Xóa sản phẩm thất bại.",
    };
  }
}
