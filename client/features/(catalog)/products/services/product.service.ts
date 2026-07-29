import { api } from "@/lib/axios";
import {
  ProductRequest,
  ProductResponse,
  ProductUpdateRequest,
  GetProductsParams,
} from "../product.type";
import { PageResponse } from "@/shared/types/page.type";

export async function createProduct(
  values: ProductRequest,
): Promise<ProductResponse> {
  const response = await api.post("/products", values);
  return response.data.result;
}

export async function getProducts(
  params?: GetProductsParams,
): Promise<PageResponse<ProductResponse>> {
  const response = await api.get("/products/admin", { params });
  const data = response.data.result;
  if (data && Array.isArray(data.content)) {
  }
  return data;
}

export async function getProduct(productId: number): Promise<ProductResponse> {
  const response = await api.get(`/products/admin/${productId}`);
  return response.data.result;
}

export async function getProductBySlug(slug: string): Promise<ProductResponse> {
  const response = await api.get(`/products/${slug}`);
  return response.data.result;
}

export async function updateProduct(
  productId: number,
  values: ProductUpdateRequest,
): Promise<ProductResponse> {
  const response = await api.patch(`/products/${productId}`, values);
  return response.data.result;
}

export async function deleteProduct(productId: number): Promise<void> {
  await api.delete(`/products/${productId}`);
}

export async function publishProduct(
  productId: number,
): Promise<ProductResponse> {
  const response = await api.patch(`/products/${productId}/publish`);
  return response.data.result;
}

export async function unpublishProduct(
  productId: number,
): Promise<ProductResponse> {
  const response = await api.post(`/products/${productId}/unpublish`);
  return response.data.result;
}

export async function previewSkus(
  productId: number,
  values: { attributes: Record<string, any[]> },
): Promise<Array<{ attributes: Record<string, any> }>> {
  const response = await api.post(
    `/products/${productId}/skus/preview`,
    values,
  );
  return response.data.result;
}

export async function createSkusBulk(
  productId: number,
  values: any[],
): Promise<any[]> {
  const response = await api.post(`/products/${productId}/skus/bulk`, values);
  return response.data.result;
}
