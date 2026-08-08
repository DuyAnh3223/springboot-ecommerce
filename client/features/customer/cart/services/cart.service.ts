import "server-only";
import { cache } from "react";
import { api } from "@/shared/http/api";
import { isAxiosError } from "axios";
import { CartSnapshot, CartItem, AddCartItemInput } from "../types/cart.types";

export class ApiError extends Error {
  code?: number;
  status?: number;

  constructor(message: string, code?: number, status?: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

const EMPTY_CART_SNAPSHOT: CartSnapshot = {
  cartId: null,
  status: "ACTIVE",
  items: [],
  userId: null,
};

export const cartService = {
  getCart: cache(async (): Promise<CartSnapshot> => {
    try {
      const response = await api.get("/cart");
      if (response.data.code === 1000 && response.data.result) {
        return response.data.result;
      }
      if (response.data.code !== 1000) {
        throw new ApiError(
          response.data.message || "Không thể tải dữ liệu giỏ hàng",
          response.data.code,
          response.status
        );
      }
      return EMPTY_CART_SNAPSHOT;
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.status === 404) {
        return EMPTY_CART_SNAPSHOT;
      }
      if (isAxiosError(error) && error.response?.data?.code) {
        throw new ApiError(
          error.response.data.message || "Lỗi tải giỏ hàng",
          error.response.data.code,
          error.response.status
        );
      }
      throw error;
    }
  }),

  async addToCart(input: AddCartItemInput): Promise<CartSnapshot> {
    try {
      const response = await api.post("/cart/add", {
        productSkuId: input.productSkuId,
        quantity: input.quantity,
      });
      if (response.data.code !== 1000) {
        throw new ApiError(
          response.data.message || "Không thể thêm sản phẩm vào giỏ hàng",
          response.data.code,
          response.status
        );
      }
      return response.data.result;
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.data?.code) {
        throw new ApiError(
          error.response.data.message || "Lỗi thêm giỏ hàng",
          error.response.data.code,
          error.response.status
        );
      }
      throw error;
    }
  },

  async updateCartItemQuantity(skuId: number, quantity: number): Promise<CartItem> {
    try {
      const response = await api.patch(`/cart/items/${skuId}`, { quantity });
      if (response.data.code !== 1000) {
        throw new ApiError(
          response.data.message || "Không thể cập nhật số lượng",
          response.data.code,
          response.status
        );
      }
      return response.data.result;
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.data?.code) {
        throw new ApiError(
          error.response.data.message || "Lỗi cập nhật số lượng",
          error.response.data.code,
          error.response.status
        );
      }
      throw error;
    }
  },

  async removeCartItem(skuId: number): Promise<void> {
    try {
      const response = await api.delete(`/cart/items/${skuId}`);
      if (response.data.code !== 1000 && response.data.code !== undefined) {
        throw new ApiError(
          response.data.message || "Không thể xóa sản phẩm khỏi giỏ hàng",
          response.data.code,
          response.status
        );
      }
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.data?.code) {
        throw new ApiError(
          error.response.data.message || "Lỗi xóa sản phẩm",
          error.response.data.code,
          error.response.status
        );
      }
      throw error;
    }
  },

  async clearCart(): Promise<void> {
    try {
      const response = await api.delete("/cart");
      if (response.data.code !== 1000 && response.data.code !== undefined) {
        throw new ApiError(
          response.data.message || "Không thể xóa toàn bộ giỏ hàng",
          response.data.code,
          response.status
        );
      }
    } catch (error: unknown) {
      if (isAxiosError(error) && error.response?.status === 404) {
        return;
      }
      if (isAxiosError(error) && error.response?.data?.code) {
        throw new ApiError(
          error.response.data.message || "Lỗi xóa giỏ hàng",
          error.response.data.code,
          error.response.status
        );
      }
      throw error;
    }
  },
};
