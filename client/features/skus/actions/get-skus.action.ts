"use server";

import { getSkus } from "../services/sku.service";
import { GetSkusParams, SkuResponse } from "../sku.type";
import { PageResponse } from "@/shared/types/page.type";

interface GetSkusActionResult {
  skus?: PageResponse<SkuResponse>;
  error?: string;
}

export async function getSkusAction(
  params?: GetSkusParams,
): Promise<GetSkusActionResult> {
  try {
    const skus = await getSkus(params);
    return { skus };
  } catch {
    return { error: "Không thể tải danh sách SKU sản phẩm." };
  }
}
