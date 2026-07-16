import { PageResponse } from "@/types/page.type";

export interface AttributeResponse {
  id: number;
  code: string;
  name: string;
  dataType: string;
  unit: string | null;
  enumValues: Record<string, any> | null;
}

export interface AttributeRequest {
  code?: string;
  name: string;
  dataType: string;
  unit?: string | null;
  enumValues?: Record<string, any> | null;
}

export interface CategoryAttributeResponse {
  id: number; // ID of the CategoryAttribute record
  attributeId: number;
  code: string;
  name: string;
  dataType: string;
  unit: string | null;
  enumValues: Record<string, any> | null;
  isFilterable: boolean;
  isVariantDefining: boolean;
  isCompatibilityKey: boolean;
  isRequired: boolean;
  sortOrder: number;
}

export interface AssignAttributeRequest {
  attributeId: number;
  isFilterable: boolean;
  isVariantDefining: boolean;
  isCompatibilityKey: boolean;
  isRequired: boolean;
  sortOrder: number;
}

export interface GetAttributesParams {
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}
