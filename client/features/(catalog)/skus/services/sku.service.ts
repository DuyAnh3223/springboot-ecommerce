import { api } from "@/lib/axios";
import {
  SkuRequest,
  SkuResponse,
  SkuUpdateRequest,
  GetSkusParams,
} from "../sku.type";
import { PageResponse } from "@/types/page.type";

export async function createSku(
  values: SkuRequest,
): Promise<SkuResponse> {
  const response = await api.post("/skus", values);
  return response.data.result;
}

export async function getSkus(
  params?: GetSkusParams,
): Promise<PageResponse<SkuResponse>> {
  const response = await api.get("/skus", { params });
  return response.data.result;
}

export async function getSku(skuId: number): Promise<SkuResponse> {
  const response = await api.get(`/skus/${skuId}`);
  return response.data.result;
}

export async function updateSku(
  skuId: number,
  values: SkuUpdateRequest,
): Promise<SkuResponse> {
  const response = await api.patch(`/skus/${skuId}`, values);
  return response.data.result;
}

export async function deleteSku(skuId: number): Promise<void> {
  await api.delete(`/skus/${skuId}`);
}
