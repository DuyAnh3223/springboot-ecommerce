import React from "react";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Search, Filter } from "lucide-react";
import { useVoucherFilters } from "../hooks/useVoucherFilters";

interface VoucherFiltersProps {
  filters: ReturnType<typeof useVoucherFilters>;
}

export function VoucherFilters({ filters }: VoucherFiltersProps) {
  const { searchTerm, setSearchTerm, selectedStatus, handleStatusChange } = filters;

  return (
    <Card className="border-none shadow-sm bg-white p-3">
      <div className="flex flex-col md:flex-row gap-4 items-center">
        <div className="relative flex-1 w-full">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <Search className="size-4.5" />
          </span>
          <Input
            type="text"
            placeholder="Tìm kiếm mã hoặc tên voucher..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 h-10 border-slate-200"
          />
        </div>
        <div className="flex gap-3 w-full md:w-auto shrink-0">
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-slate-400" />
            <select
              value={selectedStatus}
              onChange={(e) => handleStatusChange(e.target.value)}
              className="h-10 border border-slate-200 rounded-md px-3 text-sm text-slate-700 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">Tất cả trạng thái</option>
              <option value="active">Đang hoạt động</option>
              <option value="disabled">Đã tắt (Ngừng kích hoạt)</option>
              <option value="expired">Hết hạn</option>
            </select>
          </div>
        </div>
      </div>
    </Card>
  );
}
