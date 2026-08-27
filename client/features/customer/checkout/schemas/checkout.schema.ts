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

export const checkoutFormSchema = z
  .object({
    addressMode: z.enum(["EXISTING", "NEW"]),
    addressId: z.string().trim().optional(),
    newAddress: checkoutNewAddressSchema,
    voucherCode: z.string().optional(),
  })
  .superRefine((values, context) => {
    if (values.addressMode === "EXISTING" && !values.addressId) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["addressId"],
        message: "Vui lòng chọn địa chỉ nhận hàng.",
      });
    }

    if (values.addressMode === "NEW" && values.addressId) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["addressMode"],
        message: "Vui lòng chỉ chọn một hình thức địa chỉ.",
      });
    }
  });

export type CheckoutFormSchemaValues = z.infer<typeof checkoutFormSchema>;
