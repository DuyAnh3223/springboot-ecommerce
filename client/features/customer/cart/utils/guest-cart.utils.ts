import { CartItem } from "../types/cart.types";

const GUEST_CART_STORAGE_KEY = "abtechzone_guest_cart";
export const MAX_GUEST_CART_ITEMS = 20;

export function getGuestCart(): CartItem[] {
  if (typeof window === "undefined") return [];
  try {
    const data = localStorage.getItem(GUEST_CART_STORAGE_KEY);
    if (!data) return [];
    const parsed = JSON.parse(data);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error("Error reading guest cart from localStorage:", error);
    return [];
  }
}

export function saveGuestCart(items: CartItem[]): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(GUEST_CART_STORAGE_KEY, JSON.stringify(items));
  } catch (error) {
    console.error("Error saving guest cart to localStorage:", error);
  }
}

export interface AddGuestCartResult {
  success: boolean;
  items: CartItem[];
  error?: string;
}

export function addGuestCartItem(item: CartItem): AddGuestCartResult {
  const currentItems = getGuestCart();
  const existingIndex = currentItems.findIndex(
    (i) => i.productSkuId === item.productSkuId
  );

  if (existingIndex > -1) {
    const updatedItems = [...currentItems];
    updatedItems[existingIndex] = {
      ...updatedItems[existingIndex],
      quantity: updatedItems[existingIndex].quantity + item.quantity,
      unitPrice: item.unitPrice || updatedItems[existingIndex].unitPrice,
      productName: item.productName || updatedItems[existingIndex].productName,
      imageUrl: item.imageUrl || updatedItems[existingIndex].imageUrl,
    };
    saveGuestCart(updatedItems);
    return { success: true, items: updatedItems };
  }

  if (currentItems.length >= MAX_GUEST_CART_ITEMS) {
    return {
      success: false,
      items: currentItems,
      error: `Giỏ hàng tạm thời chỉ được chứa tối đa ${MAX_GUEST_CART_ITEMS} loại sản phẩm. Vui lòng đăng nhập để lưu thêm.`,
    };
  }

  const updatedItems = [...currentItems, item];
  saveGuestCart(updatedItems);
  return { success: true, items: updatedItems };
}

export function updateGuestCartItemQuantity(
  skuId: number,
  quantity: number
): CartItem[] {
  const currentItems = getGuestCart();
  const updatedItems = currentItems.map((item) =>
    item.productSkuId === skuId ? { ...item, quantity } : item
  );
  saveGuestCart(updatedItems);
  return updatedItems;
}

export function removeGuestCartItem(skuId: number): CartItem[] {
  const currentItems = getGuestCart();
  const updatedItems = currentItems.filter((item) => item.productSkuId !== skuId);
  saveGuestCart(updatedItems);
  return updatedItems;
}

export function clearGuestCart(): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.removeItem(GUEST_CART_STORAGE_KEY);
  } catch (error) {
    console.error("Error clearing guest cart from localStorage:", error);
  }
}
