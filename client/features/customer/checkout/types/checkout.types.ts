import type {
  CheckoutResponse,
  CreateCheckoutOrderRequest,
} from "@/features/orders/order.type";
import type { CheckoutFormSchemaValues } from "../schemas/checkout.schema";

export type CheckoutAddressMode = "EXISTING" | "NEW";

export interface CheckoutNewAddressForm {
  recipientName: string;
  phone: string;
  province: string;
  ward: string;
  street: string;
  saveAddress: boolean;
}

export type CheckoutFormValues = CheckoutFormSchemaValues;

export interface CheckoutActionError {
  code?: number;
  status?: number;
  message: string;
  latestReview?: CheckoutResponse;
}

export type CheckoutActionResult<T> =
  | { success: true; data: T }
  | { success: false; error: CheckoutActionError };

export interface IdempotencyAttempt {
  payloadFingerprint: string;
  idempotencyKey: string;
}

export type CheckoutCreatePayload = CreateCheckoutOrderRequest;
