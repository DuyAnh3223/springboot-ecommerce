import { z } from "zod";

export const addressSchema = z.object({
  recipientName: z.string().trim().min(1, "Vui lòng nhập tên người nhận"),
  phone: z
    .string()
    .trim()
    .min(10, "Số điện thoại phải có ít nhất 10 chữ số")
    .regex(/^[0-9]+$/, "Số điện thoại chỉ được chứa chữ số"),
  province: z.string().trim().min(1, "Vui lòng chọn Tỉnh/Thành phố"),
  district: z.string().trim().min(1, "Vui lòng chọn Quận/Huyện"),
  ward: z.string().trim().min(1, "Vui lòng chọn Phường/Xã"),
  street: z.string().trim().min(1, "Vui lòng nhập địa chỉ chi tiết"),
  ghnProvinceId: z.coerce.number().int().positive("Mã tỉnh GHN là bắt buộc"),
  ghnDistrictId: z.coerce.number().int().positive("Mã quận/huyện GHN là bắt buộc"),
  ghnWardCode: z.string().trim().min(1, "Mã phường/xã GHN là bắt buộc"),
  country: z.string(),
  isDefault: z.boolean(),
});

export type AddressInput = z.infer<typeof addressSchema>;
export const addressUpdateSchema = addressSchema;
export type AddressUpdateInput = AddressInput;
