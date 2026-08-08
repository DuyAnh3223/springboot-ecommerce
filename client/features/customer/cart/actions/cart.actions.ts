"use server";

import { revalidatePath } from "next/cache";
import { isAxiosError } from "axios";
import { cartService, ApiError } from "../services/cart.service";
import { AddCartItemInput, ActionResult, CartSnapshot } from "../types/cart.types";

interface ErrorMapping {
  message: string;
  requiresAuth: boolean;
}

function mapErrorMessage(error: unknown): ErrorMapping {
  let code: number | undefined;
  let status: number | undefined;
  let backendMsg: string | undefined;

  if (error instanceof ApiError) {
    code = error.code;
    status = error.status;
    backendMsg = error.message;
  } else if (isAxiosError(error)) {
    status = error.response?.status;
    code = error.response?.data?.code;
    backendMsg = error.response?.data?.message;
  } else if (error instanceof Error) {
    backendMsg = error.message;
  }

  if (status === 401 || code === 1006) {
    return { message: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", requiresAuth: true };
  }

  if (code === 1010) {
    return { message: "Sản phẩm không tồn tại hoặc đã ngưng kinh doanh.", requiresAuth: false };
  }

  if (code === 1015 || code === 1032 || (backendMsg && backendMsg.toLowerCase().includes("stock"))) {
    return { message: "Số lượng sản phẩm trong kho không đủ.", requiresAuth: false };
  }

  if (code === 1028) {
    return { message: "Sản phẩm không còn trong giỏ hàng.", requiresAuth: false };
  }

  if (code === 1029) {
    return { message: "Số lượng sản phẩm không hợp lệ.", requiresAuth: false };
  }

  if (code === 1031) {
    return { message: "Giỏ hàng của bạn đang trống.", requiresAuth: false };
  }

  if (code === 1033) {
    return { message: "Sản phẩm hiện ngưng kinh doanh.", requiresAuth: false };
  }

  if (code === 1030 || (backendMsg && backendMsg.toLowerCase().includes("cart not found"))) {
    return { message: "Giỏ hàng của bạn đang trống.", requiresAuth: false };
  }

  return {
    message: "Có lỗi xảy ra trong quá trình xử lý giỏ hàng. Vui lòng thử lại sau.",
    requiresAuth: false,
  };
}

export async function getCartAction(): Promise<ActionResult<CartSnapshot>> {
  try {
    const snapshot = await cartService.getCart();
    return { success: true, data: snapshot };
  } catch (error: unknown) {
    const mapped = mapErrorMessage(error);
    return { success: false, error: mapped.message, requiresAuth: mapped.requiresAuth };
  }
}

export async function addCartItemAction(input: AddCartItemInput): Promise<ActionResult<CartSnapshot>> {
  if (!input.productSkuId || input.productSkuId <= 0) {
    return { success: false, error: "Mã sản phẩm (SKU) không hợp lệ." };
  }
  if (!input.quantity || input.quantity < 1) {
    return { success: false, error: "Số lượng sản phẩm phải từ 1 trở lên." };
  }

  try {
    const snapshot = await cartService.addToCart(input);
    revalidatePath("/cart");
    return { success: true, data: snapshot };
  } catch (error: unknown) {
    const mapped = mapErrorMessage(error);
    return { success: false, error: mapped.message, requiresAuth: mapped.requiresAuth };
  }
}

export async function updateCartItemAction(
  skuId: number,
  quantity: number
): Promise<ActionResult<CartSnapshot>> {
  if (!skuId || skuId <= 0) {
    return { success: false, error: "Mã sản phẩm (SKU) không hợp lệ." };
  }
  if (!quantity || quantity < 1) {
    return { success: false, error: "Số lượng sản phẩm phải tối thiểu là 1." };
  }

  try {
    await cartService.updateCartItemQuantity(skuId, quantity);
    const snapshot = await cartService.getCart();
    revalidatePath("/cart");
    return { success: true, data: snapshot };
  } catch (error: unknown) {
    const mapped = mapErrorMessage(error);
    return { success: false, error: mapped.message, requiresAuth: mapped.requiresAuth };
  }
}

export async function removeCartItemAction(skuId: number): Promise<ActionResult<CartSnapshot>> {
  if (!skuId || skuId <= 0) {
    return { success: false, error: "Mã sản phẩm (SKU) không hợp lệ." };
  }

  try {
    await cartService.removeCartItem(skuId);
    const snapshot = await cartService.getCart();
    revalidatePath("/cart");
    return { success: true, data: snapshot };
  } catch (error: unknown) {
    const mapped = mapErrorMessage(error);
    return { success: false, error: mapped.message, requiresAuth: mapped.requiresAuth };
  }
}

export async function clearCartAction(): Promise<ActionResult<CartSnapshot>> {
  try {
    await cartService.clearCart();
    const snapshot = await cartService.getCart();
    revalidatePath("/cart");
    return { success: true, data: snapshot };
  } catch (error: unknown) {
    const mapped = mapErrorMessage(error);
    return { success: false, error: mapped.message, requiresAuth: mapped.requiresAuth };
  }
}
