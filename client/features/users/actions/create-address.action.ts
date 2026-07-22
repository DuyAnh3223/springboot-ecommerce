"use server";

import { createAddress } from "../services/address.service";
import { AddressRequest } from "../address.type";
import { revalidatePath } from "next/cache";

export async function createAddressAction(values: AddressRequest) {
  try {
    const result = await createAddress(values);
    revalidatePath("/profile");
    return { success: true, address: result };
  } catch (error: any) {
    console.error("Create address action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Thêm địa chỉ thất bại.",
    };
  }
}
