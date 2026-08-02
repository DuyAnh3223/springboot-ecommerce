"use server";

import { removeCategoryAttribute } from "../services/attribute.service";

export async function removeCategoryAttributeAction(
  categoryId: number,
  attributeId: number,
) {
  try {
    await removeCategoryAttribute(categoryId, attributeId);
    return { success: true };
  } catch (error: any) {
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Gỡ thuộc tính khỏi danh mục thất bại.",
    };
  }
}
