"use server";

import { cookies } from "next/headers";
import { SignInInput } from "@/features/auth/schemas/auth.schema";
import { signIn, getCurrentUser } from "@/features/auth/services/auth.service";

export async function signInAction(values: SignInInput) {
  try {
    const result = await signIn(values);

    if (result && result.token) {
      const { token } = result;

      const cookieStore = await cookies();
      cookieStore.set("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        sameSite: "lax",
        maxAge: 60 * 60 * 24 * 7, // 7 days
        path: "/",
      });

      // Fetch current user info
      try {
        const userResult = await getCurrentUser(token);
        return {
          success: true,
          user: userResult,
        };
      } catch (userError) {
        console.error("Fetch user info error in sign in:", userError);
        return {
          success: true,
          user: { username: values.username },
        };
      }
    }

    return { error: "Đăng nhập không thành công" };
  } catch (error: any) {
    console.error("Sign in action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error:
        backendMessage ||
        "Đăng nhập không thành công. Vui lòng kiểm tra lại tài khoản hoặc kết nối mạng.",
    };
  }
}
