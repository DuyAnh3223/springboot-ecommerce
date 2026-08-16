"use server";
import { revalidatePath } from "next/cache";
import { deleteVoucher } from "../services/voucher.service";
import { mapVoucherError } from "../utils/voucher-error.mapper";

export async function deleteVoucherAction(voucherCode: string) {
  try {
    await deleteVoucher(voucherCode);
    revalidatePath("/admin/vouchers");
    return { success: true };
  } catch (error: unknown) {
    return {
      error: mapVoucherError(error, "Ngừng kích hoạt mã giảm giá thất bại"),
    };
  }
}
