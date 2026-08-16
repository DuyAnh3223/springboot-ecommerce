"use server";
import { revalidatePath } from "next/cache";
import { updateVoucher } from "../services/voucher.service";
import { VoucherUpdateRequest } from "../voucher.type";
import { mapVoucherError } from "../utils/voucher-error.mapper";

export async function updateVoucherAction(
  voucherCode: string,
  values: VoucherUpdateRequest,
) {
  try {
    const result = await updateVoucher(voucherCode, values);
    revalidatePath("/admin/vouchers");
    return { success: true, voucher: result };
  } catch (error: unknown) {
    return {
      error: mapVoucherError(error, "Cập nhật mã giảm giá thất bại"),
    };
  }
}
