"use server";

import { getAddresses } from "../services/address.service";
import { GetAddressesParams } from "../address.type";

export async function getAddressesAction(params?: GetAddressesParams) {
  try {
    const result = await getAddresses(params);
    return { success: true, page: result };
  } catch (error: any) {
    console.error("Get addresses action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Lấy danh sách địa chỉ thất bại.",
    };
  }
}
