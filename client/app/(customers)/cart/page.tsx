import React from "react";
import { getUserSession } from "@/features/auth/actions/get-session.action";
import { cartService } from "@/features/customer/cart/services/cart.service";
import { CartPageClient } from "@/features/customer/cart/components/CartPageClient";
export const metadata = {
  title: "Giỏ hàng | ABTechZone",
  description: "Xem và quản lý các sản phẩm trong giỏ hàng của bạn tại ABTechZone",
};

export default async function CartPage() {
  const session = await getUserSession();
  let initialError: string | null = null;

  if (session) {
    try {
      await cartService.getCart();
    } catch {
      initialError = "Không thể kết nối đến máy chủ để tải giỏ hàng. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.";
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <h1 className="text-2xl font-black text-slate-900 md:text-3xl">
          Giỏ Hàng Của Bạn
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Quản lý danh sách sản phẩm và chuẩn bị cho đơn hàng của bạn.
        </p>
      </div>

      <CartPageClient initialError={initialError} isGuest={!session} />
    </div>
  );
}
