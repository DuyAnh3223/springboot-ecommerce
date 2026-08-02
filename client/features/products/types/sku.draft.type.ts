export type SellingMode = "single" | "multi";

export interface SkuImageDraft {
  url: string;
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
    images?: SkuImageDraft[];
  }[];
  removedSkuIds: number[];
}
