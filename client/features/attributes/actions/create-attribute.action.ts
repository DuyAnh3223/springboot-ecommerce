"use server";

import { createAttribute as createAttributeService } from "../services/attribute.service";
import { AttributeRequest } from "../attribute.type";

export async function createAttributeAction(values: AttributeRequest) {
  try {
    const result = await createAttributeService(values);
    return { success: true, attribute: result };
  } catch (error: any) {
    console.error("Create attribute action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Tạo thuộc tính thất bại.",
    };
  }
}
