"use server";

import { getAttributesByCategoryId as getAttributesByCategoryIdService } from "../services/attribute.service";
import { GetAttributesParams } from "../attribute.type";

export async function getAttributesAction(
  categoryId: number,
  params?: GetAttributesParams,
) {
  try {
    const result = await getAttributesByCategoryIdService(categoryId, params);
    return { success: true, data: result };
  } catch (error: any) {
    console.error("Get attributes action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tải danh sách thuộc tính thất bại.",
    };
  }
}
