export interface SkuResponse {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  price: number;
  stock: number;
  imageUrl: string | null;
  isActive?: boolean;
  attributes: Record<string, any>;
}

export interface SkuRequest {
  productId: number;
  sku: string;
  price: number;
  stock: number;
  currency?: string;
  weightGram?: number;
  imageUrl?: string | null;
  attributes?: Record<string, any> | null;
}

export interface SkuUpdateRequest {
  sku?: string;
  price?: number;
  stock?: number;
  currency?: string;
  weightGram?: number;
  imageUrl?: string | null;
  isActive?: boolean;
  attributes?: Record<string, any> | null;
}

export interface GetSkusParams {
  search?: string;
  productId?: number;
  minPrice?: number;
  maxPrice?: number;
  minStock?: number;
  maxStock?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
  currency?: string;
  weightGram?: number;
}
