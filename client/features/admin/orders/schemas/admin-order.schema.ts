import { z } from "zod";
import { normalizeAdminNote } from "../utils/admin-order.utils.ts";

export const adminOrderStatusSchema = z.object({
  status: z.enum(["PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "DELIVERY_FAILED", "CANCELLED"]),
  note: z.string().max(500, "Ghi chú không được vượt quá 500 ký tự.").transform(normalizeAdminNote),
}).superRefine((value, ctx) => {
  if ((value.status === "CANCELLED" || value.status === "DELIVERY_FAILED") && !value.note) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ["note"],
      message: value.status === "DELIVERY_FAILED" ? "Vui lòng nhập lý do giao thất bại." : "Vui lòng nhập lý do hủy đơn.",
    });
  }
});

export type AdminOrderStatusFormValues = z.infer<typeof adminOrderStatusSchema>;
