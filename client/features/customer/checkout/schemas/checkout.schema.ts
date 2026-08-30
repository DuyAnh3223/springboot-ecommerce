import { z } from "zod";

const requiredText = (message: string) => z.string().trim().min(1, message);

export const checkoutNewAddressSchema = z.object({
  recipientName: requiredText("Vui lòng nhập tên người nhận."),
  phone: z
    .string()
    .trim()
    .min(10, "Số điện thoại phải có ít nhất 10 chữ số.")
    .regex(/^[0-9]+$/, "Số điện thoại chỉ được chứa chữ số."),
  province: requiredText("Vui lòng nhập tỉnh/thành phố."),
  ward: requiredText("Vui lòng nhập phường/xã."),
  street: requiredText("Vui lòng nhập địa chỉ chi tiết."),
  saveAddress: z.boolean(),
});

const checkoutSharedFields = {
  voucherCode: z.string().optional(),
};

const inactiveNewAddressSchema = z.object({
  recipientName: z.string().optional(),
  phone: z.string().optional(),
  province: z.string().optional(),
  ward: z.string().optional(),
  street: z.string().optional(),
  saveAddress: z.boolean().optional(),
});

export const checkoutFormSchema = z.discriminatedUnion("addressMode", [
  z.object({
    ...checkoutSharedFields,
    addressMode: z.literal("EXISTING"),
    addressId: requiredText("Vui lòng chọn địa chỉ nhận hàng."),
    newAddress: inactiveNewAddressSchema.optional(),
  }),
  z.object({
    ...checkoutSharedFields,
    addressMode: z.literal("NEW"),
    addressId: z.string().trim().max(0, "Vui lòng chỉ chọn một hình thức địa chỉ.").optional(),
    newAddress: checkoutNewAddressSchema,
  }),
]);

export type CheckoutFormSchemaValues = z.infer<typeof checkoutFormSchema>;
