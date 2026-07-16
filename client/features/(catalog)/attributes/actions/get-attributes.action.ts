"use server";

import { getCategoryAttributes } from "../services/attribute.service";

export async function getAttributesAction(categoryId: number) {
  try {
    const result = await getCategoryAttributes(categoryId);
    return { success: true, data: result };
  } catch (error: any) {
    console.error("Get category attributes action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tải danh sách thuộc tính danh mục thất bại.",
    };
  }
}
