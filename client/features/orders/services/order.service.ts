import { api } from "@/shared/http/api";
import { OrderResponse } from "../order.type";

export async function getUserOrders(userId: string): Promise<OrderResponse[]> {
  const response = await api.get(`/orders/user/${userId}`);
  return response.data.result;
}
