export interface CartItem {
  productSkuId: number;
  skuCode: string;
  productName: string;
  imageUrl: string;
  quantity: number;
  unitPrice: number;
}

export interface CartSnapshot {
  cartId: number | null;
  status: string;
  items: CartItem[];
  userId: string | null;
}

export interface AddCartItemInput {
  productSkuId: number;
  quantity: number;
}

export interface UpdateCartItemInput {
  quantity: number;
}

export interface ActionResult<T = void> {
  success: boolean;
  data?: T;
  error?: string;
  requiresAuth?: boolean;
}
