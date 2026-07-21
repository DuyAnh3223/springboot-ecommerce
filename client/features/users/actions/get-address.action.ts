"use server";

import { getAddress } from "../services/address.service";

export async function getAddressAction(addressId: string) {
  try {
    const result = await getAddress(addressId);
    return { success: true, address: result };
  } catch (error: any) {
    console.error("Get address action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Lấy thông tin địa chỉ thất bại.",
    };
  }
}
