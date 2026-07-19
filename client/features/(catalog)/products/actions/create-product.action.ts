"use server";

import { createProduct as createProductService } from "../services/product.service";
import { ProductRequest } from "../product.type";

export async function createProductAction(values: ProductRequest) {
  try {
    const result = await createProductService(values);
    return { success: true, product: result };
  } catch (error: any) {
    console.error("Create product action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tạo sản phẩm thất bại.",
    };
  }
}
