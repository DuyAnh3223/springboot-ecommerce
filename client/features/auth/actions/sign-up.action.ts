"use server";

import { SignUpInput } from "@/features/auth/schemas/auth.schema";
import { signUp } from "@/features/auth/services/auth.service";

export async function signUpAction(values: SignUpInput) {
  try {
    await signUp(values);
    return { success: true };
  } catch (error: any) {
    console.error("Sign up action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    if (error.response?.data?.code === 1001) {
      return { error: "Tài khoản đã tồn tại" };
    }
    return {
      error: backendMessage || "Đăng ký không thành công. Vui lòng thử lại.",
    };
  }
}
