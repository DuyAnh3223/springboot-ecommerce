export interface OrderResponse {
  orderId: number;
  orderCode: string;
  orderStatus: string;
  subtotal: number;
  shippingFee: number;
  totalDiscount: number;
  totalCheckout: number;
}
