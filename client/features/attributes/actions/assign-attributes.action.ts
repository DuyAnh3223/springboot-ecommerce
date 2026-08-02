"use server";

import { assignCategoryAttributes } from "../services/attribute.service";
import { AssignAttributeRequest } from "../attribute.type";

export async function assignCategoryAttributesAction(
  categoryId: number,
  requests: AssignAttributeRequest[],
) {
  try {
    const result = await assignCategoryAttributes(categoryId, requests);
    return { success: true, data: result };
  } catch (error: any) {
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Gán thuộc tính vào danh mục thất bại.",
    };
  }
}
