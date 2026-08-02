"use server";

import { updateCategoryAttribute } from "../services/attribute.service";
import { AssignAttributeRequest } from "../attribute.type";

export async function updateCategoryAttributeAction(
  categoryId: number,
  id: number,
  request: AssignAttributeRequest,
) {
  try {
    const result = await updateCategoryAttribute(categoryId, id, request);
    return { success: true, data: result };
  } catch (error: any) {
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Cập nhật cấu hình thuộc tính thất bại.",
    };
  }
}
