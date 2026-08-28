import { z } from "zod";

export const cancelOrderSchema = z.object({
  reason: z
    .string()
    .trim()
    .min(1, "Vui lòng nhập lý do hủy đơn hàng.")
    .max(500, "Lý do hủy không được vượt quá 500 ký tự."),
});

export type CancelOrderFormValues = z.infer<typeof cancelOrderSchema>;
