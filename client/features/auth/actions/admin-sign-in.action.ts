"use server";

import { cookies } from "next/headers";
import { SignInInput } from "@/features/auth/schemas/auth.schema";
import { signIn, getCurrentUser } from "@/features/auth/services/auth.service";

export async function adminSignInAction(values: SignInInput) {
  try {
    const result = await signIn(values);

    if (result && result.token) {
      const { token } = result;

      // Fetch user info first to check role before setting the cookie
      const userResult = await getCurrentUser(token);
      
      const isAdmin = userResult.roles?.some((role: any) => role.name === "ADMIN");
      if (!isAdmin) {
        return { error: "Tài khoản không có quyền truy cập trang quản trị." };
      }

      const cookieStore = await cookies();
      cookieStore.set("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax",
        maxAge: 60 * 60 * 24 * 7, // 7 days
        path: "/",
      });

      return {
        success: true,
        user: userResult,
      };
    }

    return { error: "Đăng nhập không thành công" };
  } catch (error: any) {
    console.error("Admin sign in action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error:
        backendMessage ||
        "Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản hoặc kết nối mạng.",
    };
  }
}
