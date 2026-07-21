import { api } from "@/lib/axios";
import { OrderResponse } from "../order.type";

export async function getUserOrders(userId: string): Promise<OrderResponse[]> {
  const response = await api.get(`/orders/user/${userId}`);
  return response.data.result;
}
