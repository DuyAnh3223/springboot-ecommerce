"use server";

import { deleteAddress } from "../services/address.service";
import { revalidatePath } from "next/cache";

export async function deleteAddressAction(addressId: string) {
  try {
    await deleteAddress(addressId);
    revalidatePath("/profile");
    return { success: true };
  } catch (error: any) {
    console.error("Delete address action error:", error);
    const backendMessage = error.response?.data?.message || error.message;
    return {
      error: backendMessage || "Xóa địa chỉ thất bại.",
    };
  }
}
