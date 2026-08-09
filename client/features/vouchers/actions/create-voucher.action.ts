"use server";
import { revalidatePath } from "next/cache";
import { createVoucher } from "../services/voucher.service";
import { VoucherCreateRequest } from "../voucher.type";

export async function createVoucherAction(values: VoucherCreateRequest) {
  try {
    const result = await createVoucher(values);
    revalidatePath("/admin/vouchers");
    return { success: true, voucher: result };
  } catch (error: any) {
    return { error: error.response?.data?.message || "Thêm voucher thất bại" };
  }
}
