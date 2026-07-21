import { z } from "zod";

export const addressSchema = z.object({
  recipientName: z.string().min(1, "Vui lòng nhập tên người nhận"),
  phone: z
    .string()
    .min(10, "Số điện thoại phải có ít nhất 10 chữ số")
    .regex(/^[0-9]+$/, "Số điện thoại chỉ được chứa ký số"),
  province: z.string().min(1, "Vui lòng chọn Tỉnh/Thành phố"),
  district: z.string().min(1, "Vui lòng chọn Quận/Huyện"),
  ward: z.string().min(1, "Vui lòng chọn Phường/Xã"),
  streetAddress: z.string().min(1, "Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường)"),
  country: z.string(),
  isDefault: z.boolean(),
});

export type AddressInput = z.infer<typeof addressSchema>;

export const addressUpdateSchema = addressSchema;
export type AddressUpdateInput = AddressInput;

