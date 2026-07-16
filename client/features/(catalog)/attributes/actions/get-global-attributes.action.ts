"use server";

import { getGlobalAttributes as getGlobalAttributesService } from "../services/attribute.service";
import { GetAttributesParams } from "../attribute.type";

export async function getGlobalAttributesAction(params?: GetAttributesParams) {
  try {
    const result = await getGlobalAttributesService(params);
    return { success: true, data: result };
  } catch (error: any) {
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tải danh sách thuộc tính thất bại.",
    };
  }
}
