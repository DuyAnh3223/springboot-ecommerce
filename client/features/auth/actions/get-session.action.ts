"use server";

import { cookies } from "next/headers";
import { getCurrentUser } from "@/features/auth/services/auth.service";

export async function getSession() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("token")?.value;

    if (!token) return null;

    const userResult = await getCurrentUser(token);
    return userResult;
  } catch (error) {
    console.error("Get session error:", error);
    return null;
  }
}
