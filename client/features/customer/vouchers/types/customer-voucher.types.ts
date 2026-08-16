export type CustomerVoucherFilterTab =
  | "ALL"
  | "PERCENTAGE"
  | "FIXED_AMOUNT"
  | "SCOPE_ALL"
  | "SCOPE_SPECIFIC";

export interface CustomerVoucherCardProps {
  voucher: {
    code: string;
    name: string;
    description?: string | null;
    type: "FIXED_AMOUNT" | "PERCENTAGE";
    value: number;
    maxDiscountAmount?: number | null;
    startDate: string;
    endDate: string;
    minOrderValue?: number | null;
    maxUses?: number | null;
    usedCount?: number | null;
    maxPerUser?: number | null;
    applyScope: "ALL" | "SPECIFIC";
    productSkus?: Array<{
      id: number;
      sku: string;
      price: number;
      stock?: number;
      productName?: string;
    }>;
  };
  onApply?: (code: string) => void;
  isApplied?: boolean;
  disabled?: boolean;
  disabledReason?: string;
}
