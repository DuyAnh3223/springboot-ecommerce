import { PageResponse } from "@/types/page.type";

export interface AttributeResponse {
  id: number;
  categoryId: number;
  code: string;
  name: string;
  dataType: string;
  unit: string | null;
  enumValues: Record<string, any> | null;
  isFilterable: boolean;
  isVariantDefining: boolean;
  isCompatibilityKey: boolean;
  sortOrder: number;
}

export interface AttributeRequest {
  categoryId: number;
  name: string;
  dataType: string;
  unit?: string | null;
  enumValues?: Record<string, any> | null;
  isFilterable?: boolean;
  isVariantDefining?: boolean;
  isCompatibilityKey?: boolean;
  sortOrder?: number;
}

export interface GetAttributesParams {
  keyword?: string;
  isFilterable?: boolean;
  isVariantDefining?: boolean;
  isCompatibilityKey?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}
