"use server";

import { deleteCategory as deleteCategoryService } from "../services/category.service";

export async function deleteCategoryAction(categoryId: number) {
  try {
    await deleteCategoryService(categoryId);
    return { success: true };
  } catch (error: any) {
    console.error("Delete category action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Xóa danh mục thất bại.",
    };
  }
}
