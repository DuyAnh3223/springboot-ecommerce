"use server";

import { revalidatePath } from "next/cache";
import { getUserSession } from "@/features/auth/actions";
import { cancelOrderSchema } from "@/features/customer/orders/schemas/cancel-order.schema";
import {
  getOrderErrorMessage,
  shouldRefreshOrderAfterError,
} from "../order-error";
import { cancelOrder, OrderApiError } from "../services/order.service";

export type CancelOrderActionResult =
  | { success: true; message: string }
  | {
      success: false;
      error: {
        message: string;
        code?: number;
        status?: number;
        refresh: boolean;
      };
    };

export async function cancelOrderAction(
  orderCode: string,
  input: unknown,
): Promise<CancelOrderActionResult> {
  const session = await getUserSession();
  if (!session) {
    return {
      success: false,
      error: {
        message: getOrderErrorMessage({ status: 401 }),
        status: 401,
        refresh: false,
      },
    };
  }

  const parsed = cancelOrderSchema.safeParse(input);
  if (!parsed.success) {
    return {
      success: false,
      error: {
        message: parsed.error.issues[0]?.message ?? "Lý do hủy đơn hàng chưa hợp lệ.",
        status: 400,
        refresh: false,
      },
    };
  }

  try {
    await cancelOrder(orderCode, parsed.data);
    revalidatePath("/profile/orders");
    revalidatePath(`/profile/orders/${encodeURIComponent(orderCode)}`);
    return {
      success: true,
      message: "Đơn hàng đã được hủy. Tồn kho và voucher đang được cập nhật từ hệ thống.",
    };
  } catch (error: unknown) {
    const orderError = error instanceof OrderApiError
      ? { code: error.code, status: error.status }
      : {};
    return {
      success: false,
      error: {
        ...orderError,
        message: getOrderErrorMessage(orderError),
        refresh: shouldRefreshOrderAfterError(orderError),
      },
    };
  }
}
