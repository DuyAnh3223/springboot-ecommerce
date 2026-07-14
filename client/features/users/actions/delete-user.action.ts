"use server";

import { deleteUser as deleteUserService } from "../services/user.service";

export async function deleteUserAction(userId: string) {
  try {
    await deleteUserService(userId);
    return { success: true };
  } catch (error: any) {
    console.error("Delete user action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Khóa tài khoản thất bại.",
    };
  }
}
