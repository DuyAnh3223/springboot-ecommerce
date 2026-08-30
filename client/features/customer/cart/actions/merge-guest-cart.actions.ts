"use server";

import { revalidatePath } from "next/cache";
import { cartService, ApiError } from "../services/cart.service";
import {
  ActionResult,
  CartMergeItemInput,
  CartMergeResponse,
  CartSnapshot,
} from "../types/cart.types";

export interface MergeGuestCartActionData {
  merge: CartMergeResponse;
  cart: CartSnapshot;
}

export async function mergeGuestCartAction(
  mergeId: string,
  guestItems: CartMergeItemInput[],
): Promise<ActionResult<MergeGuestCartActionData>> {
  if (!mergeId || !guestItems || guestItems.length === 0) {
    return { success: false, error: "Giỏ hàng tạm thời không có sản phẩm hợp lệ." };
  }

  try {
    const merge = await cartService.mergeGuestCart(mergeId, guestItems);
    const cart = await cartService.getCart();
    revalidatePath("/cart");
    return { success: true, data: { merge, cart } };
  } catch (error: unknown) {
    console.error("Error merging guest cart:", error);
    if (error instanceof ApiError && error.status === 401) {
      return {
        success: false,
        requiresAuth: true,
        error: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
      };
    }
    return {
      success: false,
      error: "Không thể đồng bộ giỏ hàng tạm thời vào tài khoản. Vui lòng thử lại sau.",
    };
  }
}
