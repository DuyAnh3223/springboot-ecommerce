import { z } from "zod";

export const userUpdateSchema = z.object({
  firstName: z.string().min(1, "Vui lòng nhập tên"),
  lastName: z.string().min(1, "Vui lòng nhập tên"),
  phone: z.string().min(10, "Vui lòng nhập số điện thoại"),
});

export type UserUpdateInput = z.infer<typeof userUpdateSchema>;
