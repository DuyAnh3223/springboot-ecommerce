import { z } from "zod";

const optionalNumber = (schema: z.ZodNumber) =>
  z.preprocess((val) => {
    if (val === "" || val === null || val === undefined) return null;
    const num = Number(val);
    return isNaN(num) ? null : num;
  }, schema.nullable().optional());

export const voucherRawObject = z.object({
  name: z.string().min(1, "Tên voucher không được để trống"),
  description: z.string().optional().nullable(),
  type: z.enum(["FIXED_AMOUNT", "PERCENTAGE"]),
  value: z.coerce.number().positive("Giá trị giảm giá phải lớn hơn 0"),
  maxDiscountAmount: optionalNumber(z.number().positive("Trần giảm giá phải lớn hơn 0")),
  code: z
    .string()
    .min(1, "Mã voucher không được để trống")
    .regex(/^[A-Z0-9_]+$/, "Mã chỉ chứa chữ in hoa, số và dấu gạch dưới _"),
  startDate: z.string().min(1, "Ngày bắt đầu là bắt buộc"),
  endDate: z.string().min(1, "Ngày kết thúc là bắt buộc"),
  maxUses: optionalNumber(z.number().int().min(0, "Lượt sử dụng tối đa phải >= 0")),
  maxPerUser: optionalNumber(z.number().int().min(0, "Lượt sử dụng/khách hàng phải >= 0")),
  minOrderValue: optionalNumber(z.number().min(0, "Đơn hàng tối thiểu phải >= 0")),
  isActive: z.boolean().default(true),
  applyScope: z.enum(["ALL", "SPECIFIC"]),
  productSkuIds: z.array(z.number()).optional().default([]),
});

export const createVoucherSchema = voucherRawObject
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
  )
  .refine(
    (data) => {
      if (!data.startDate) return true;
      return new Date(data.startDate) > new Date();
    },
    {
      message: "Ngày bắt đầu phải ở trong tương lai",
      path: ["startDate"],
    }
  )
  .transform((data) => ({
    ...data,
    maxDiscountAmount: data.type === "FIXED_AMOUNT" ? null : (data.maxDiscountAmount ?? null),
  }));

export const updateVoucherSchema = voucherRawObject
  .omit({ code: true })
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
  )
  .transform((data) => ({
    ...data,
    maxDiscountAmount: data.type === "FIXED_AMOUNT" ? null : (data.maxDiscountAmount ?? null),
  }));

export type VoucherFormInput = z.input<typeof voucherRawObject>;
export type VoucherCreateInput = z.input<typeof createVoucherSchema>;
export type VoucherCreateOutput = z.output<typeof createVoucherSchema>;
export type VoucherUpdateInput = z.input<typeof updateVoucherSchema>;
export type VoucherUpdateOutput = z.output<typeof updateVoucherSchema>;
