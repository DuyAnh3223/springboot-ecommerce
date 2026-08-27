export interface OrderResponse {
  orderId: number;
  orderCode: string;
  orderStatus: string;
  subtotal: number;
  shippingFee: number;
  totalDiscount: number;
  totalCheckout: number;
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

export interface OrderDetailResponse {
  id: number;
  orderCode: string;
  status: string;
  paymentMethod: string;
  paymentStatus: string;
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
  items: Array<{
    skuId: number;
    skuCode: string;
    productName: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
  }>;
}
