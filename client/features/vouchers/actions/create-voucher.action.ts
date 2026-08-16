"use server";
import { revalidatePath } from "next/cache";
import { createVoucher } from "../services/voucher.service";
import { VoucherCreateRequest } from "../voucher.type";
import { mapVoucherError } from "../utils/voucher-error.mapper";

export async function createVoucherAction(values: VoucherCreateRequest) {
  try {
    const result = await createVoucher(values);
    revalidatePath("/admin/vouchers");
    return { success: true, voucher: result };
  } catch (error: unknown) {
    return {
      error: mapVoucherError(error, "Tạo mới mã giảm giá thất bại"),
    };
  }
}
