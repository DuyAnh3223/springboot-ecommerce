import { z } from "zod";

export const ProductSchema = z.object({
  name: z.string().min(1, "Vui lòng nhập tên sản phẩm"),
  slug: z
    .string()
    .min(1, "Vui lòng nhập slug")
    .regex(
      /^[a-z0-9]+(?:-[a-z0-9]+)*$/,
      "Slug chỉ gồm chữ thường, số và dấu gạch ngang",
    ),
  categoryId: z.number().min(1, "Vui lòng chọn danh mục"),
  description: z.string().optional().or(z.literal("")),
  thumbnail: z
    .string()
    .url("URL ảnh không hợp lệ")
    .optional()
    .or(z.literal("")),
});

export type ProductFormValues = z.infer<typeof ProductSchema>;
