"use server";

import { updateProduct as updateProductService } from "../services/product.service";
import { ProductUpdateRequest } from "../product.type";

export async function updateProductAction(productId: number, values: ProductUpdateRequest) {
  try {
    const result = await updateProductService(productId, values);
    return { success: true, product: result };
  } catch (error: any) {
    console.error("Update product action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Cập nhật sản phẩm thất bại.",
    };
  }
}
