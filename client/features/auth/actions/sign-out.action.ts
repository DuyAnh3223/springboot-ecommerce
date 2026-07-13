"use server";

import { cookies } from "next/headers";
import { signOut } from "@/features/auth/services/auth.service";

export async function signoutAction() {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get("token")?.value;

    if (token) {
      try {
        await signOut(token);
      } catch (backendError) {
        console.error("Error logging out from backend:", backendError);
      }
    }

    cookieStore.delete("token");
    return { success: true };
  } catch (error) {
    console.error("Sign out action error:", error);
    return { error: "Đăng xuất không thành công" };
  }
}
