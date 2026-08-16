import React from "react";
import { getVouchers } from "@/features/vouchers/services/voucher.service";
import { CustomerVoucherHero } from "@/features/customer/vouchers/components/CustomerVoucherHero";
import { CustomerVoucherList } from "@/features/customer/vouchers/components/CustomerVoucherList";
import { VoucherResponse } from "@/features/vouchers/voucher.type";

export const metadata = {
  title: "Mã Giảm Giá & Khuyến Mãi | ABTechZone",
  description:
    "Tổng hợp mã voucher giảm giá, ưu đãi hấp dẫn khi mua linh kiện PC, CPU, VGA, Mainboard và phụ kiện máy tính tại ABTechZone.",
};

export default async function CustomerVouchersPage() {
  let vouchers: VoucherResponse[] = [];

  try {
    const res = await getVouchers({
      active: true,
      status: "active",
      size: 50,
      sortBy: "id",
      order: "desc",
    });
    if (res?.content) {
      vouchers = res.content;
    }
  } catch (error) {
    console.error("Lỗi khi tải danh sách voucher cho khách hàng:", error);
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 min-h-[calc(100vh-200px)]">
      <CustomerVoucherHero />
      <CustomerVoucherList initialVouchers={vouchers} />
    </div>
  );
}
