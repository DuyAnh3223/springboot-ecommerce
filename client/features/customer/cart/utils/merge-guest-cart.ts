import type {
  CartItem,
  CartMergeItemResult,
  GuestMergeNotice,
} from "../types/cart.types";

export const GUEST_CART_MERGE_REASON_MESSAGES: Record<string, string> = {
  SKU_NOT_FOUND: "Sản phẩm không còn tồn tại hoặc đã bị xóa.",
  SKU_INACTIVE: "Sản phẩm hiện đang ngừng kinh doanh.",
  PRODUCT_NOT_SELLABLE: "Sản phẩm hiện chưa thể bán.",
  QUANTITY_OVERFLOW: "Số lượng sản phẩm vượt quá giới hạn cho phép.",
  INSUFFICIENT_STOCK: "Số lượng sản phẩm trong kho không đủ.",
};

export function getGuestMergeReasonMessage(reasonCode: string | null): string {
  return (
    (reasonCode && GUEST_CART_MERGE_REASON_MESSAGES[reasonCode]) ||
    "Sản phẩm chưa được đồng bộ vào tài khoản."
  );
}

export interface AppliedGuestMergeResult {
  retainedItems: CartItem[];
  notices: GuestMergeNotice[];
  allResultsApplied: boolean;
}

export function applyGuestMergeResult(
  localItems: CartItem[],
  results: CartMergeItemResult[],
): AppliedGuestMergeResult {
  const resultBySku = new Map(results.map((result) => [result.skuId, result]));
  const retainedItems = localItems.filter((item) => {
    return resultBySku.get(item.productSkuId)?.status !== "MERGED";
  });
  const notices = results
    .filter((result) => result.status === "REJECTED")
    .map((result) => {
      const localItem = localItems.find((item) => item.productSkuId === result.skuId);
      return {
        skuId: result.skuId,
        quantity: localItem?.quantity ?? result.requestedQuantity,
        reasonCode: result.reasonCode,
        message: getGuestMergeReasonMessage(result.reasonCode),
      };
    });
  const localSkuIds = new Set(localItems.map((item) => item.productSkuId));
  const allResultsApplied =
    results.length === localSkuIds.size &&
    localItems.every((item) => resultBySku.has(item.productSkuId));

  return { retainedItems, notices, allResultsApplied };
}
