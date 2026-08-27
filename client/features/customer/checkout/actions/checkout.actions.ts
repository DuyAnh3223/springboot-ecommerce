"use server";

import {
  createCheckoutOrder,
  OrderApiError,
  reviewCheckout,
} from "@/features/orders/services/order.service";
import type {
  CheckoutResponse,
  CheckoutReviewRequest,
  CheckoutOrderResponse,
} from "@/features/orders/order.type";
import { getCheckoutErrorMessage } from "../utils/checkout.utils";
import type {
  CheckoutActionError,
  CheckoutActionResult,
  CheckoutCreatePayload,
} from "../types/checkout.types";

function toCheckoutError(error: unknown): CheckoutActionError {
  if (error instanceof OrderApiError) {
    return {
      code: error.code,
      status: error.status,
      message: getCheckoutErrorMessage(error.code, error.status),
      latestReview: error.code === 1068 ? (error.result as CheckoutResponse | undefined) : undefined,
    };
  }

  return {
    message: "Không thể kết nối đến hệ thống checkout. Vui lòng thử lại sau.",
  };
}

export async function reviewCheckoutAction(
  request: CheckoutReviewRequest,
): Promise<CheckoutActionResult<CheckoutResponse>> {
  try {
    return { success: true, data: await reviewCheckout(request) };
  } catch (error: unknown) {
    return { success: false, error: toCheckoutError(error) };
  }
}

export async function createCheckoutOrderAction(
  request: CheckoutCreatePayload,
  idempotencyKey: string,
): Promise<CheckoutActionResult<CheckoutOrderResponse>> {
  try {
    return {
      success: true,
      data: await createCheckoutOrder(request, idempotencyKey),
    };
  } catch (error: unknown) {
    return { success: false, error: toCheckoutError(error) };
  }
}
