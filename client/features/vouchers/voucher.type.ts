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
  skuCode: string;
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
  isActive?: boolean;
  applyScope: VoucherApplyScope;
  productSkuIds?: number[];
}

export interface VoucherUpdateRequest extends VoucherCreateRequest {}

export interface VoucherSearchParams {
  active?: boolean;
  status?: "active" | "expired";
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}

export interface VoucherDiscountRequest {
  code: string;
  totalOrder: number;
  productSkuIds?: number[];
  userId?: string;
}

export interface VoucherDiscountResponse {
  discountAmount: number;
  totalOrder: number;
  totalPrice: number;
}
