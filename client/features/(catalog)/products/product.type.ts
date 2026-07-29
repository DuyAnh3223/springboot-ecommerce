import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { SkuResponse } from "@/features/(catalog)/skus/sku.type";

export interface BrandResponse {
  id: number;
  name: string;
  slug: string;
  logoUrl: string | null;
}

export interface ProductResponse {
  id: number;
  name: string;
  slug: string;
  primaryImageUrl: string | null;
  description: string | null;
  rating: number | null;
  draft: boolean;
  published: boolean;
  category: CategoryResponse | null;
  brand: BrandResponse | null;
  attributes: Record<string, any>;
  skus?: SkuResponse[];
  skuCount?: number;
  activeSkuCount?: number;
  totalStock?: number;
  priceMin?: number;
  priceMax?: number;
}

export interface ProductRequest {
  name: string;
  description?: string | null;
  draft?: boolean;
  published?: boolean;
  categoryId?: number | null;
  brandId?: number | null;
  attributes?: Record<string, any> | null;
}

export interface ProductUpdateRequest {
  name?: string;
  description?: string | null;
  draft?: boolean;
  published?: boolean;
  categoryId?: number | null;
  brandId?: number | null;
  attributes?: Record<string, any> | null;
}

export interface GetProductsParams {
  search?: string;
  categoryId?: number;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
  status?: "draft" | "published" | "all";
}
