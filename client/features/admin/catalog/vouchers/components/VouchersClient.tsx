"use client";

import React from "react";
import { Ticket, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { VoucherResponse } from "@/features/vouchers/voucher.type";
import { PageResponse } from "@/shared/types/page.type";
import { useVoucherFilters } from "../hooks/useVoucherFilters";
import { useVoucherDialogStore } from "../stores/voucher-dialog.store";
import { VoucherFilters } from "./VoucherFilters";
import { VoucherTable } from "./VoucherTable";
import { VoucherModals } from "./VoucherModals";
import PaginationBar from "@/features/admin/catalog/categories/components/PaginationBar";

interface VouchersClientProps {
  initialData: PageResponse<VoucherResponse>;
}

export function VouchersClient({ initialData }: VouchersClientProps) {
  const filters = useVoucherFilters();
  const { openDialog } = useVoucherDialogStore();

  return (
    <div className="space-y-4">
      {/* Header toolbar */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            <Ticket className="size-6 text-blue-600" /> Quản lý Mã giảm giá
          </h1>
          <p className="text-sm text-slate-500">
            Tạo, xem và cấu hình danh sách mã voucher giảm giá cho đơn hàng và sản phẩm.
          </p>
        </div>
        <Button
          onClick={() => openDialog("create")}
          className="bg-blue-600 hover:bg-blue-700 text-white gap-2 cursor-pointer h-10"
        >
          <Plus className="size-4" /> Thêm mã giảm giá
        </Button>
      </div>

      {/* Filters */}
      <VoucherFilters filters={filters} />

      {/* Voucher Table */}
      <VoucherTable vouchers={initialData.content} />

      {/* Pagination Bar */}
      <PaginationBar
        initialData={initialData}
        currentPage={filters.currentPage}
        currentSize={filters.currentSize}
        isPending={filters.isPending}
        onPageChange={(p) => filters.updateQueryParams({ page: p })}
        onSizeChange={(val) => filters.updateQueryParams({ size: val, page: 1 })}
      />

      {/* Dialog Modals */}
      <VoucherModals />
    </div>
  );
}
