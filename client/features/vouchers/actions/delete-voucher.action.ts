"use server";
import { revalidatePath } from "next/cache";
import { deleteVoucher } from "../services/voucher.service";

export async function deleteVoucherAction(voucherCode: string) {
  try {
    const result = await deleteVoucher(voucherCode);
    revalidatePath("/admin/vouchers");
    return { success: true, voucher: result };
  } catch (error: any) {
    return {
      error: error.response?.data?.message || "Xóa voucher thất bại",
    };
  }
}
