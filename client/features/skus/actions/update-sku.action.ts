"use server";

import { updateSku } from "../services/sku.service";
import { SkuResponse, SkuUpdateRequest } from "../sku.type";

export async function updateSkuAction(
  skuId: number,
  values: SkuUpdateRequest,
): Promise<{ sku?: SkuResponse; error?: string }> {
  try {
    const sku = await updateSku(skuId, values);
    return { sku };
  } catch (err: any) {
    console.error("Lỗi khi cập nhật SKU:", err);
    return {
      error:
        err.response?.data?.message ||
        "Cập nhật thông tin biến thể SKU thất bại.",
    };
  }
}
