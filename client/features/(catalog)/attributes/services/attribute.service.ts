import { api } from "@/lib/axios";
import {
  AttributeRequest,
  AttributeResponse,
  GetAttributesParams,
} from "../attribute.type";
import { PageResponse } from "@/types/page.type";

export async function createAttribute(
  values: AttributeRequest,
): Promise<AttributeResponse> {
  const response = await api.post("/attributes", values);
  return response.data.result;
}

export async function getAttributesByCategoryId(
  categoryId: number,
  params?: GetAttributesParams,
): Promise<PageResponse<AttributeResponse>> {
  const response = await api.get(`/attributes/category/${categoryId}`, { params });
  return response.data.result;
}

export async function getAttribute(
  attributeId: number,
): Promise<AttributeResponse> {
  const response = await api.get(`/attributes/${attributeId}`);
  return response.data.result;
}

export async function updateAttribute(
  attributeId: number,
  values: AttributeRequest,
): Promise<AttributeResponse> {
  const response = await api.patch(`/attributes/${attributeId}`, values);
  return response.data.result;
}

export async function deleteAttribute(attributeId: number): Promise<void> {
  await api.delete(`/attributes/${attributeId}`);
}
