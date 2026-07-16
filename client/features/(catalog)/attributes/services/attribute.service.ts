import { api } from "@/lib/axios";
import {
  AttributeRequest,
  AttributeResponse,
  CategoryAttributeResponse,
  AssignAttributeRequest,
  GetAttributesParams,
} from "../attribute.type";
import { PageResponse } from "@/types/page.type";

export async function createAttribute(
  values: AttributeRequest,
): Promise<AttributeResponse> {
  const response = await api.post("/attributes", values);
  return response.data.result;
}

export async function getGlobalAttributes(
  params?: GetAttributesParams,
): Promise<PageResponse<AttributeResponse>> {
  const response = await api.get("/attributes", { params });
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

// ==========================================
// CATEGORY ATTRIBUTE SERVICE
// ==========================================

export async function getCategoryAttributes(
  categoryId: number,
): Promise<CategoryAttributeResponse[]> {
  const response = await api.get(`/categories/${categoryId}/attributes`);
  return response.data.result;
}

export async function assignCategoryAttributes(
  categoryId: number,
  requests: AssignAttributeRequest[],
): Promise<CategoryAttributeResponse[]> {
  const response = await api.post(
    `/categories/${categoryId}/attributes`,
    requests,
  );
  return response.data.result;
}

export async function updateCategoryAttribute(
  categoryId: number,
  id: number,
  request: AssignAttributeRequest,
): Promise<CategoryAttributeResponse> {
  const response = await api.patch(
    `/categories/${categoryId}/attributes/${id}`,
    request,
  );
  return response.data.result;
}

export async function removeCategoryAttribute(
  categoryId: number,
  attributeId: number,
): Promise<void> {
  await api.delete(`/categories/${categoryId}/attributes/${attributeId}`);
}
