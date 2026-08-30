import "server-only";
import { isAxiosError } from "axios";
import { api } from "@/shared/http/api";
import {
  CancelOrderRequest,
  AdminOrderListQuery,
  AdminOrderStatusUpdateRequest,
  CheckoutOrderResponse,
  CheckoutResponse,
  CheckoutReviewRequest,
  CreateCheckoutOrderRequest,
  OrderListQuery,
  OrderDetailResponse,
  OrderSummaryResponse,
  OrderMutationResponse,
} from "../order.type";
import type { PageResponse } from "@/shared/types/page.type";

export class OrderApiError extends Error {
  constructor(
    message: string,
    public readonly code?: number,
    public readonly status?: number,
    public readonly result?: unknown,
  ) {
    super(message);
    this.name = "OrderApiError";
  }
}

function unwrapResult<T>(response: { data: { code?: number; message?: string; result?: T } }): T {
  if (response.data.code !== undefined && response.data.code !== 1000) {
    throw new OrderApiError(response.data.message || "Order request failed", response.data.code);
  }

  return response.data.result as T;
}

function rethrowOrderError(error: unknown): never {
  if (error instanceof OrderApiError) {
    throw error;
  }

  if (isAxiosError(error)) {
    throw new OrderApiError(
      error.response?.data?.message || "Order request failed",
      error.response?.data?.code,
      error.response?.status,
      error.response?.data?.result,
    );
  }

  throw error;
}

export async function getMyOrders(
  query: OrderListQuery,
): Promise<PageResponse<OrderSummaryResponse>> {
  try {
    const response = await api.get("/orders/me", {
      params: {
        page: query.page,
        size: query.size,
        status: query.status,
      },
    });
    return unwrapResult<PageResponse<OrderSummaryResponse>>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function getAdminOrders(
  query: AdminOrderListQuery,
): Promise<PageResponse<OrderSummaryResponse>> {
  try {
    const response = await api.get("/admin/orders", {
      params: {
        search: query.search,
        status: query.status,
        fromDate: query.fromDate,
        toDate: query.toDate,
        page: query.page,
        size: query.size,
      },
    });
    return unwrapResult<PageResponse<OrderSummaryResponse>>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function getAdminOrderDetail(orderCode: string): Promise<OrderDetailResponse> {
  try {
    const response = await api.get(`/admin/orders/${encodeURIComponent(orderCode)}`);
    return unwrapResult<OrderDetailResponse>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function updateAdminOrderStatus(
  orderCode: string,
  request: AdminOrderStatusUpdateRequest,
): Promise<OrderMutationResponse> {
  try {
    const response = await api.patch(
      `/admin/orders/${encodeURIComponent(orderCode)}/status`,
      request,
    );
    return unwrapResult<OrderMutationResponse>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function reviewCheckout(
  request: CheckoutReviewRequest,
): Promise<CheckoutResponse> {
  try {
    const response = await api.post("/orders/checkout-review", request);
    return unwrapResult<CheckoutResponse>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function createCheckoutOrder(
  request: CreateCheckoutOrderRequest,
  idempotencyKey: string,
): Promise<CheckoutOrderResponse> {
  try {
    const response = await api.post("/orders", request, {
      headers: { "Idempotency-Key": idempotencyKey },
    });
    return unwrapResult<CheckoutOrderResponse>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function getOrderDetail(orderCode: string): Promise<OrderDetailResponse> {
  try {
    const response = await api.get(`/orders/${encodeURIComponent(orderCode)}`);
    return unwrapResult<OrderDetailResponse>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}

export async function cancelOrder(
  orderCode: string,
  request: CancelOrderRequest,
): Promise<CheckoutOrderResponse> {
  try {
    const response = await api.post(
      `/orders/${encodeURIComponent(orderCode)}/cancel`,
      request,
    );
    return unwrapResult<CheckoutOrderResponse>(response);
  } catch (error: unknown) {
    rethrowOrderError(error);
  }
}
