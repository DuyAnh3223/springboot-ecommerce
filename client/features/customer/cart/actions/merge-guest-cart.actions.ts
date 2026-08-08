"use server";

import { revalidatePath } from "next/cache";
import { cartService } from "../services/cart.service";
import { ActionResult, CartSnapshot } from "../types/cart.types";

export interface MergeGuestCartItemInput {
  productSkuId: number;
  quantity: number;
}

export async function mergeGuestCartAction(
  guestItems: MergeGuestCartItemInput[]
): Promise<ActionResult<CartSnapshot>> {
  if (!guestItems || guestItems.length === 0) {
    try {
      const snapshot = await cartService.getCart();
      return { success: true, data: snapshot };
    } catch {
      return { success: true };
    }
  }

  try {
    for (const item of guestItems) {
      if (item.productSkuId > 0 && item.quantity > 0) {
        try {
          await cartService.addToCart({
            productSkuId: item.productSkuId,
            quantity: item.quantity,
          });
        } catch (itemError) {
          console.warn(`Could not merge SKU ${item.productSkuId}:`, itemError);
        }
      }
    }

    const snapshot = await cartService.getCart();
    revalidatePath("/cart");
    return { success: true, data: snapshot };
  } catch (error: unknown) {
    console.error("Error merging guest cart:", error);
    return {
      success: false,
      error: "Không thể đồng bộ giỏ hàng tạm thời vào tài khoản.",
    };
  }
}
