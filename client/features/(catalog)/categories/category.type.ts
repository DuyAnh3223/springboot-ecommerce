import { PageResponse } from "@/types/page.type";

export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  thumbnail: string | null;
  active: boolean;
  parentId?: number;
}

export interface CategoryRequest {
  name: string;
  slug: string;
  thumbnail?: string;
}

export interface CategoryUpdateRequest {
  name: string;
  slug: string;
  thumbnail?: string;
}

export interface GetCategoriesParams {
  keyword?: string;
  isActive?: boolean;
  parentId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}
