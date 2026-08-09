import { z } from "zod";

export const voucherSchema = z
  .object({
    name: z.string().min(1, "Tên voucher không được để trống"),
    description: z.string().optional(),
    type: z.enum(["FIXED_AMOUNT", "PERCENTAGE"]),
    value: z.coerce.number().positive("Giá trị phải lớn hơn 0"),
    maxDiscountAmount: z.coerce.number().positive().nullable().optional(),
    code: z
      .string()
      .min(1, "Mã voucher không được để trống")
      .regex(/^[A-Z0-9_]+$/, "Mã chỉ chứa chữ in hoa, số và _"),
    startDate: z.string().min(1, "Ngày bắt đầu là bắt buộc"),
    endDate: z.string().min(1, "Ngày kết thúc là bắt buộc"),
    maxUses: z.coerce.number().int().min(0).nullable().optional(),
    maxPerUser: z.coerce.number().int().min(0).nullable().optional(),
    minOrderValue: z.coerce.number().min(0).nullable().optional(),
    isActive: z.boolean().default(true),
    applyScope: z.enum(["ALL", "SPECIFIC"]),
    productSkuIds: z.array(z.number()).optional().default([]),
  })
  .refine(
    (data) => {
      if (!data.startDate || !data.endDate) return true;
      return new Date(data.endDate) > new Date(data.startDate);
    },
    {
      message: "Ngày kết thúc phải sau ngày bắt đầu",
      path: ["endDate"],
    }
  )
  .refine(
    (data) => {
      if (data.type === "PERCENTAGE") {
        return data.value < 100;
      }
      return true;
    },
    {
      message: "Phần trăm giảm giá phải nhỏ hơn 100",
      path: ["value"],
    }
  )
  .refine(
    (data) => {
      if (data.applyScope === "SPECIFIC") {
        return Array.isArray(data.productSkuIds) && data.productSkuIds.length > 0;
      }
      return true;
    },
    {
      message: "Phải chọn ít nhất 1 sản phẩm khi áp dụng phạm vi cụ thể",
      path: ["productSkuIds"],
    }
  );

export type VoucherValues = z.infer<typeof voucherSchema>;

