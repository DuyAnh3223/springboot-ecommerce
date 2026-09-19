import type { ProductImageRequest } from "@/features/skus/sku.type";

export type SellingMode = "single" | "multi";

export interface SkuImageDraft {
  id?: number;
  url: string;
  previewUrl?: string;
  file?: File;
  isPrimary?: boolean;
  sortOrder?: number;
}

export interface SkuDraft {
  id?: number;
  sku: string;
  price: number;
  stock: number;
  weightGram?: number;
  currency?: string;
  attributes: Record<string, unknown>;
  images?: SkuImageDraft[];
  isNew?: boolean;
}

export interface ProductReconcilePayload {
  skus: {
    id?: number;
    sku: string;
    price: number;
    stock: number;
    weightGram?: number;
    currency?: string;
    attributes: Record<string, unknown>;
    images?: ProductImageRequest[];
  }[];
  removedSkuIds: number[];
}
