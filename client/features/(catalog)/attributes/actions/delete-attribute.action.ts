"use server";

import { deleteAttribute as deleteAttributeService } from "../services/attribute.service";

export async function deleteAttributeAction(attributeId: number) {
  try {
    await deleteAttributeService(attributeId);
    return { success: true };
  } catch (error: any) {
    console.error("Delete attribute action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Xóa thuộc tính thất bại.",
    };
  }
}
