"use server";

import { updateAddress } from "../services/address.service";
import { AddressRequest } from "../address.type";
import { revalidatePath } from "next/cache";

export async function updateAddressAction(addressId: string, values: AddressRequest) {
  try {
    const result = await updateAddress(addressId, values);
    revalidatePath("/profile");
    return { success: true, address: result };
  } catch (error: any) {
    console.error("Update address action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Cập nhật địa chỉ thất bại.",
    };
  }
}
