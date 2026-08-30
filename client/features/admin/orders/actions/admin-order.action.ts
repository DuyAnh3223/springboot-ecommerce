"use server";

import { revalidatePath } from "next/cache";
import { getAdminSession } from "@/features/auth/actions";
import { updateAdminOrderStatus } from "@/features/orders/services/order.service";
import { adminOrderStatusSchema } from "../schemas/admin-order.schema";
import { getAdminOrderErrorMessage, shouldRefreshAdminOrderAfterError } from "../utils/admin-order-error.utils";

export async function updateAdminOrderStatusAction(orderCode: string, input: unknown) {
  const session = await getAdminSession();
  if (!session) return { ok: false as const, error: "Bạn không có quyền cập nhật đơn hàng.", refresh: false };
  const parsed = adminOrderStatusSchema.safeParse(input);
  if (!parsed.success) return { ok: false as const, error: parsed.error.issues[0]?.message || "Thông tin cập nhật chưa hợp lệ.", refresh: false };
  try {
    await updateAdminOrderStatus(orderCode, parsed.data);
    revalidatePath("/admin/orders");
    revalidatePath(`/admin/orders/${encodeURIComponent(orderCode)}`);
    return { ok: true as const };
  } catch (error) {
    return { ok: false as const, error: getAdminOrderErrorMessage(error), refresh: shouldRefreshAdminOrderAfterError(error) };
  }
}
