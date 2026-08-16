"use server";
import { revalidatePath } from "next/cache";
import { reactivateVoucher } from "../services/voucher.service";
import { mapVoucherError } from "../utils/voucher-error.mapper";

export async function reactivateVoucherAction(voucherCode: string) {
  try {
    const result = await reactivateVoucher(voucherCode);
    revalidatePath("/admin/vouchers");
    return { success: true, voucher: result };
  } catch (error: unknown) {
    return {
      error: mapVoucherError(error, "Kích hoạt lại mã giảm giá thất bại"),
    };
  }
}
