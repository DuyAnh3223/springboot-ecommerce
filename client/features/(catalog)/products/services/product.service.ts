import { api } from "@/lib/axios";
import {
  ProductRequest,
  ProductResponse,
  ProductUpdateRequest,
  GetProductsParams,
} from "../product.type";
import { PageResponse } from "@/types/page.type";

function mapResponse(product: any): ProductResponse {
  if (!product) return product;
  return {
    ...product,
    isDraft: product.draft !== undefined ? product.draft : product.isDraft,
    isPublished:
      product.published !== undefined ? product.published : product.isPublished,
  };
}

export async function createProduct(
  values: ProductRequest,
): Promise<ProductResponse> {
  const response = await api.post("/products", values);
  return mapResponse(response.data.result);
}

export async function getProducts(
  params?: GetProductsParams,
): Promise<PageResponse<ProductResponse>> {
  const response = await api.get("/products/admin", { params });
  const data = response.data.result;
  if (data && Array.isArray(data.content)) {
    data.content = data.content.map(mapResponse);
  }
  return data;
}

export async function getProduct(productId: number): Promise<ProductResponse> {
  const response = await api.get(`/products/admin/${productId}`);
  return mapResponse(response.data.result);
}

export async function getProductBySlug(slug: string): Promise<ProductResponse> {
  const response = await api.get(`/products/${slug}`);
  return mapResponse(response.data.result);
}

export async function updateProduct(
  productId: number,
  values: ProductUpdateRequest,
): Promise<ProductResponse> {
  const response = await api.patch(`/products/${productId}`, values);
  return mapResponse(response.data.result);
}

export async function deleteProduct(productId: number): Promise<void> {
  await api.delete(`/products/${productId}`);
}

export async function publishProduct(
  productId: number,
): Promise<ProductResponse> {
  const response = await api.patch(`/products/${productId}/publish`);
  return mapResponse(response.data.result);
}

export async function unpublishProduct(
  productId: number,
): Promise<ProductResponse> {
  const response = await api.post(`/products/${productId}/unpublish`);
  return mapResponse(response.data.result);
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
