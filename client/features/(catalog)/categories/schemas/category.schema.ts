import { z } from "zod";

export const categorySchema = z.object({
  name: z.string().min(1, "Vui lòng nhập tên danh mục"),
  slug: z
    .string()
    .min(1, "Vui lòng nhập slug")
    .regex(
      /^[a-z0-9]+(?:-[a-z0-9]+)*$/,
      "Slug chỉ gồm chữ thường, số và dấu gạch ngang"
    ),
  thumbnail: z.string().url("URL ảnh không hợp lệ").optional().or(z.literal("")),
});

export type CategoryFormValues = z.infer<typeof categorySchema>;
