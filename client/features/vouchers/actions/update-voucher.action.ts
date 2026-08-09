"use server";
import { revalidatePath } from "next/cache";
import { updateVoucher } from "../services/voucher.service";
import { VoucherUpdateRequest } from "../voucher.type";

export async function updateVoucherAction(
  voucherCode: string,
  values: VoucherUpdateRequest,
) {
  try {
    const result = await updateVoucher(voucherCode, values);
    revalidatePath("/admin/vouchers");
    return { success: true, voucher: result };
  } catch (error: any) {
    return {
      error: error.response?.data?.message || "Cập nhập voucher thất bại",
    };
  }
}
