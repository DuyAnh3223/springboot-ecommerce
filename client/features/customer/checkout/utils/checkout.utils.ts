import type {
  CheckoutResponse,
  CreateCheckoutOrderRequest,
} from "@/features/orders/order.type";
import type {
  CheckoutActionError,
  CheckoutCreatePayload,
  CheckoutFormValues,
  IdempotencyAttempt,
} from "../types/checkout.types";

export function normalizeSelectedSkuIds(value?: string | string[]): number[] {
  const rawValues = Array.isArray(value) ? value : value ? [value] : [];

  return [...new Set(
    rawValues
      .flatMap((raw) => raw.split(","))
      .map((raw) => Number.parseInt(raw.trim(), 10))
      .filter((id) => Number.isInteger(id) && id > 0),
  )].sort((left, right) => left - right);
}

export function buildCheckoutUrl(selectedSkuIds: number[], voucherCode?: string | null): string {
  const normalized = [...new Set(selectedSkuIds)].sort((left, right) => left - right);
  const params = new URLSearchParams({ skuIds: normalized.join(",") });
  const normalizedVoucherCode = normalizeVoucherCode(voucherCode);
  if (normalizedVoucherCode) {
    params.set("voucherCode", normalizedVoucherCode);
  }
  return `/checkout?${params.toString()}`;
}

export function buildSignInCallbackUrl(checkoutUrl: string): string {
  return `/sign-in?callbackUrl=${encodeURIComponent(checkoutUrl)}`;
}

export function normalizeVoucherCode(value?: string | null): string | undefined {
  const normalized = value?.trim().toUpperCase();
  return normalized || undefined;
}

export function buildCreateCheckoutOrderRequest(
  review: CheckoutResponse,
  values: CheckoutFormValues,
): CheckoutCreatePayload {
  const request: CreateCheckoutOrderRequest = {
    reviewedCheckout: {
      items: review.items.map((item) => ({
        skuId: item.skuId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        lineTotal: item.lineTotal,
      })),
      subtotal: review.subtotal,
      eligibleSubtotal: review.eligibleSubtotal,
      shippingFee: review.shippingFee,
      discountAmount: review.discountAmount,
      totalAmount: review.totalAmount,
      voucher: review.voucher
        ? {
            code: review.voucher.code,
            applicable: review.voucher.applicable,
          }
        : null,
      canPlaceOrder: review.canPlaceOrder,
    },
    addressId: values.addressMode === "EXISTING" ? values.addressId || null : null,
    newUserAddress:
      values.addressMode === "NEW"
        ? {
            recipientName: values.newAddress.recipientName.trim(),
            phone: values.newAddress.phone.trim(),
            province: values.newAddress.province.trim(),
            ward: values.newAddress.ward.trim(),
            street: values.newAddress.street.trim(),
            saveAddress: values.newAddress.saveAddress,
          }
        : null,
    paymentMethod: "COD",
  };

  return request;
}

function stablePayloadFingerprint(payload: CheckoutCreatePayload): string {
  return JSON.stringify(payload);
}

export function getOrCreateIdempotencyAttempt(
  current: IdempotencyAttempt | null,
  payload: CheckoutCreatePayload,
  createKey: () => string,
): IdempotencyAttempt {
  const payloadFingerprint = stablePayloadFingerprint(payload);
  if (current?.payloadFingerprint === payloadFingerprint) {
    return current;
  }

  return {
    payloadFingerprint,
    idempotencyKey: createKey(),
  };
}

export function getCheckoutErrorMessage(code?: number, status?: number): string {
  switch (code) {
    case 1006:
      return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
    case 1032:
      return "Một số sản phẩm không còn đủ số lượng trong kho. Vui lòng kiểm tra lại.";
    case 1033:
      return "Một số sản phẩm hiện không còn kinh doanh.";
    case 1035:
    case 1036:
    case 1037:
      return "Địa chỉ nhận hàng không hợp lệ hoặc không còn khả dụng.";
    case 1038:
      return "Voucher đã đạt giới hạn sử dụng của tài khoản.";
    case 1044:
      return "Hệ thống đang bận. Vui lòng thử lại sau ít phút.";
    case 1067:
      return "Yêu cầu đặt hàng này đã được dùng cho một dữ liệu khác. Vui lòng xem lại đơn hàng.";
    case 1068:
      return "Thông tin checkout đã thay đổi. Vui lòng xem lại trước khi đặt hàng.";
    case 1022:
    case 1024:
    case 1025:
      return "Voucher không còn hợp lệ. Vui lòng kiểm tra hoặc gỡ voucher.";
    case 1034:
      return "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này.";
    default:
      if (status === 401) {
        return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
      }
      if (status === 503) {
        return "Hệ thống đang bận. Vui lòng thử lại sau ít phút.";
      }
      return "Không thể xử lý checkout lúc này. Vui lòng kiểm tra lại thông tin và thử lại.";
  }
}

export type CreateFailureResolution =
  | "RECONFIRM_LATEST_REVIEW"
  | "REFRESH_BEFORE_NEW_ATTEMPT"
  | "RETRY_SAME_ATTEMPT";

export function getCreateFailureResolution(
  error: CheckoutActionError,
): CreateFailureResolution {
  if (error.code === 1068 && error.latestReview) {
    return "RECONFIRM_LATEST_REVIEW";
  }
  if (error.code === 1067) {
    return "REFRESH_BEFORE_NEW_ATTEMPT";
  }
  return "RETRY_SAME_ATTEMPT";
}

function issueCodeToMessage(issueCode: string | null | undefined): string | null {
  switch (issueCode) {
    case "SKU_NOT_FOUND":
      return "Sản phẩm không còn tồn tại.";
    case "SKU_INACTIVE":
      return "Phiên bản sản phẩm này đã ngừng bán.";
    case "PRODUCT_NOT_AVAILABLE":
      return "Sản phẩm hiện không còn kinh doanh.";
    case "INSUFFICIENT_STOCK":
      return "Số lượng tồn kho hiện không đủ.";
    case "VOUCHER_NOT_FOUND":
      return "Voucher không tồn tại hoặc đã bị gỡ.";
    case "VOUCHER_EXPIRED":
      return "Voucher đã hết hạn sử dụng.";
    case "VOUCHER_PER_USER_LIMIT_REACHED":
      return "Tài khoản đã đạt giới hạn sử dụng voucher này.";
    default:
      return issueCode ? "Một điều kiện của checkout chưa được đáp ứng." : null;
  }
}

export function getReviewIssueMessages(review: CheckoutResponse): string[] {
  const messages = review.items
    .map((item) => issueCodeToMessage(item.issueCode))
    .filter((message): message is string => Boolean(message));
  const voucherMessage = issueCodeToMessage(review.voucher?.issueCode);
  if (voucherMessage) messages.push(voucherMessage);
  if (!review.canPlaceOrder && messages.length === 0) {
    messages.push("Một số điều kiện đặt hàng chưa được đáp ứng.");
  }

  return [...new Set(messages)];
}
