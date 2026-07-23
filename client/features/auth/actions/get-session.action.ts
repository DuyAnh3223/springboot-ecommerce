"use server";

import { cookies } from "next/headers";
import { getCurrentUser } from "@/features/auth/services/auth.service";

export async function getUserSession() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("token")?.value;

    if (!token) return null;

    const userResult = await getCurrentUser(token);
    return userResult;
  } catch (error: any) {
    // Token hết hạn hoặc không hợp lệ → xóa cookie để tự động logout ngầm
    if (error.response?.status === 401) {
      const cookieStore = await cookies();
      cookieStore.delete("token");
    }
    return null;
  }
}

export async function getAdminSession() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("token")?.value;

    if (!token) return null;

    const userResult = await getCurrentUser(token);

    const isAdmin = userResult.roles?.some(
      (role: any) => role.name === "ADMIN",
    );
    if (!isAdmin) return null;

    return userResult;
  } catch (error: any) {
    // Token expired → delete cookie for auto logout
    if (error.response?.status === 401) {
      const cookieStore = await cookies();
      cookieStore.delete("token");
    }
    return null;
  }
}
