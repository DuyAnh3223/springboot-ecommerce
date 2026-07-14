"use server";

import { createCategory as createCategoryService } from "../services/category.service";
import { CategoryRequest } from "../category.type";

export async function createCategoryAction(values: CategoryRequest) {
  try {
    const result = await createCategoryService(values);
    return { success: true, category: result };
  } catch (error: any) {
    console.error("Create category action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tạo danh mục thất bại.",
    };
  }
}
