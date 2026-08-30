export type VoucherType = "FIXED_AMOUNT" | "PERCENTAGE";
export type VoucherApplyScope = "ALL" | "SPECIFIC";

export interface VoucherResponse {
  name: string;
  description: string | null;
  type: VoucherType;
  value: number;
  maxDiscountAmount: number | null;
  code: string;
  startDate: string;
  endDate: string;
  maxUses: number | null;
  maxPerUser: number | null;
  minOrderValue: number | null;
  isActive: boolean;
  applyScope: VoucherApplyScope;
  productSkus: ProductSkuSummary[];
  usedCount: number | null;
}

export interface ProductSkuSummary {
  id: number;
  sku: string;
  price: number;
  stock: number;
  productName?: string;
}

export interface VoucherCreateRequest {
  name: string;
  description?: string | null;
  type: VoucherType;
  value: number;
  maxDiscountAmount?: number | null;
  code: string;
  startDate: string;
  endDate: string;
  maxUses?: number | null;
  maxPerUser?: number | null;
  minOrderValue?: number | null;
  active?: boolean;
  applyScope: VoucherApplyScope;
  productSkuIds?: number[];
}

export type VoucherUpdateRequest = Omit<VoucherCreateRequest, "code">;

export interface VoucherSearchParams {
  search?: string;
  active?: boolean;
  status?: "active" | "expired" | "disabled";
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}
