"use server";

import { updateCategory as updateCategoryService } from "../services/category.service";
import { CategoryUpdateRequest } from "../category.type";

export async function updateCategoryAction(
  categoryId: number,
  values: CategoryUpdateRequest
) {
  try {
    const result = await updateCategoryService(categoryId, values);
    return { success: true, category: result };
  } catch (error: any) {
    console.error("Update category action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Cập nhật danh mục thất bại.",
    };
  }
}
