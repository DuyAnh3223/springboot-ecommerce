"use server";

import { updateAddress } from "../services/address.service";
import { AddressUpdateRequest } from "../address.type";
import { revalidatePath } from "next/cache";
import { isAxiosError } from "axios";

export async function updateAddressAction(addressId: string, values: AddressUpdateRequest) {
  try {
    const result = await updateAddress(addressId, values);
    revalidatePath("/profile");
    return { success: true, address: result };
  } catch (error) {
    console.error("Update address action error:", error);
    const responseMessage = isAxiosError<{ message?: unknown }>(error)
      ? error.response?.data?.message
      : undefined;
    const backendMessage = typeof responseMessage === "string" && responseMessage
      ? responseMessage
      : error instanceof Error ? error.message : undefined;
    return {
      error: backendMessage || "Cập nhật địa chỉ thất bại.",
    };
  }
}
