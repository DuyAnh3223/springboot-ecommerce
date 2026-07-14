import { api } from "@/lib/axios";
import {
  CategoryRequest,
  CategoryResponse,
  CategoryUpdateRequest,
  GetCategoriesParams,
} from "../category.type";
import { PageResponse } from "@/types/page.type";

export async function createCategory(
  values: CategoryRequest,
): Promise<CategoryResponse> {
  const response = await api.post("/categories", values);
  return response.data.result;
}

export async function getCategories(
  params?: GetCategoriesParams,
): Promise<PageResponse<CategoryResponse>> {
  const response = await api.get("/categories", { params });
  return response.data.result;
}

export async function getCategory(
  categoryId: number,
): Promise<CategoryResponse> {
  const response = await api.get(`/categories/${categoryId}`);
  return response.data.result;
}

export async function updateCategory(
  categoryId: number,
  values: CategoryUpdateRequest,
): Promise<CategoryResponse> {
  const response = await api.patch(`/categories/${categoryId}`, values);
  return response.data.result;
}

export async function deleteCategory(categoryId: number): Promise<void> {
  await api.delete(`/categories/${categoryId}`);
}
