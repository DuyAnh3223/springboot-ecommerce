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
  } catch (error) {
    console.error("Get user session error:", error);
    return null;
  }
}

export async function getAdminSession() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("token")?.value;

    if (!token) return null;

    const userResult = await getCurrentUser(token);
    
    const isAdmin = userResult.roles?.some((role: any) => role.name === "ADMIN");
    if (!isAdmin) return null;

    return userResult;
  } catch (error) {
    console.error("Get admin session error:", error);
    return null;
  }
}
