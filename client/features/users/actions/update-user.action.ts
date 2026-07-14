"use server";

import { updateUser as updateUserService } from "../services/user.service";
import { UserUpdateRequest } from "../user.type";

export async function updateUserAction(userId: string, values: UserUpdateRequest) {
  try {
    const result = await updateUserService(userId, values);
    return { success: true, user: result };
  } catch (error: any) {
    console.error("Update user action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Cập nhật tài khoản thất bại.",
    };
  }
}
