"use server";

import { updateAttribute as updateAttributeService } from "../services/attribute.service";
import { AttributeRequest } from "../attribute.type";

export async function updateAttributeAction(
  attributeId: number,
  values: AttributeRequest,
) {
  try {
    const result = await updateAttributeService(attributeId, values);
    return { success: true, attribute: result };
  } catch (error: any) {
    console.error("Update attribute action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Cập nhật thuộc tính thất bại.",
    };
  }
}
