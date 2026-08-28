export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "SHIPPING"
  | "DELIVERED"
  | "CANCELLED";

export type OrderPaymentStatus = "UNPAID" | "PAID" | "CANCELLED";

export interface OrderItemSnapshot {
  skuId: number;
  skuCode: string;
  productName: string;
  imageUrl: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderSummaryResponse {
  id: number;
  orderCode: string;
  status: OrderStatus;
  paymentMethod: string;
  paymentStatus: OrderPaymentStatus;
  createdAt: string;
  updatedAt: string;
  subtotalAmount: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  itemCount: number;
  allowedTransitions: OrderStatus[];
  previewItem: OrderItemSnapshot | null;
}

export interface OrderHistoryResponse {
  fromStatus: OrderStatus | null;
  toStatus: OrderStatus | null;
  status: OrderStatus | null;
  actorType: string | null;
  note: string | null;
  createdAt: string;
}

export interface OrderListQuery {
  status?: OrderStatus;
  page: number;
  size: number;
}

export interface AdminOrderListQuery {
  search?: string;
  status?: OrderStatus;
  fromDate?: string;
  toDate?: string;
  page: number;
  size: number;
}

export interface AdminOrderStatusUpdateRequest {
  status: OrderStatus;
  note?: string;
}

export interface CancelOrderRequest {
  reason: string;
}

export interface CheckoutReviewRequest {
  selectedSkuIds: number[];
  voucherCode?: string;
}

export interface CheckoutItemResponse {
  skuId: number;
  skuCode: string;
  productName: string;
  imageUrl: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  availableStock: number | null;
  issueCode: string | null;
}

export interface VoucherReviewResponse {
  code: string | null;
  applicable: boolean;
  issueCode: string | null;
}

export interface CheckoutResponse {
  items: CheckoutItemResponse[];
  subtotal: number;
  eligibleSubtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  voucher: VoucherReviewResponse | null;
  canPlaceOrder: boolean;
}

export interface ReviewedCheckoutItemRequest {
  skuId: number;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface ReviewedVoucherRequest {
  code?: string | null;
  applicable: boolean;
}

export interface ReviewedCheckoutRequest {
  items: ReviewedCheckoutItemRequest[];
  subtotal: number;
  eligibleSubtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  voucher: ReviewedVoucherRequest | null;
  canPlaceOrder: boolean;
}

export interface CheckoutNewAddressRequest {
  recipientName: string;
  phone: string;
  province: string;
  ward: string;
  street: string;
  saveAddress: boolean;
}

export interface CreateCheckoutOrderRequest {
  reviewedCheckout: ReviewedCheckoutRequest;
  addressId: string | null;
  newUserAddress: CheckoutNewAddressRequest | null;
  paymentMethod: "COD";
}

export interface CheckoutOrderResponse {
  id: number;
  orderCode: string;
  status: string;
  subtotalAmount: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
}

export type OrderMutationResponse = CheckoutOrderResponse;

export interface OrderDetailResponse {
  id: number;
  orderCode: string;
  status: OrderStatus;
  paymentMethod: string;
  paymentStatus: OrderPaymentStatus;
  createdAt: string;
  updatedAt: string;
  subtotalAmount: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  recipientName: string;
  phone: string;
  fullAddress: string;
  voucherCode: string | null;
  allowedTransitions: OrderStatus[];
  items: OrderItemSnapshot[];
  history: OrderHistoryResponse[];
}
