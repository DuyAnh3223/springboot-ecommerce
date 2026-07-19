import { z } from "zod";

export const SkuSchema = z.object({
  productId: z.number().min(1, "Vui lòng chọn sản phẩm"),
  sku: z.string().min(1, "Vui lòng nhập mã SKU"),
  price: z.number().min(0, "Giá phải lớn hơn hoặc bằng 0"),
  stock: z.number().min(0, "Tồn kho phải lớn hơn hoặc bằng 0"),
  imageUrl: z
    .string()
    .url("URL ảnh không hợp lệ")
    .optional()
    .or(z.literal("")),
  attributes: z.record(z.any()).optional(),
});

export type SkuFormValues = z.infer<typeof SkuSchema>;
