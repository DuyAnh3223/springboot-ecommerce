export interface ProductImage {
  id: number;
  url: string;
  accessUrl?: string | null;
  sortOrder: number;
  primary: boolean;
  altText: string | null;
}

export interface ProductImageRequest {
  id?: number;
  url?: string;
  sortOrder?: number;
  primary?: boolean;
  altText?: string | null;
  mimeType?: string;
}

export interface SkuResponse {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  price: number;
  stock: number;
  weightGram?: number | null;
  currency?: string | null;
  imageUrl: string | null;
  images: ProductImage[];
  active?: boolean;
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
  images?: ProductImageRequest[];
  attributes?: Record<string, any> | null;
}

export interface SkuUpdateRequest {
  sku?: string;
  price?: number;
  stock?: number;
  currency?: string;
  weightGram?: number;
  images?: ProductImageRequest[];
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
