import React from "react";
import { VoucherResponse } from "@/features/vouchers/voucher.type";
import { useVoucherDialogStore } from "../stores/voucher-dialog.store";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Edit2, Power, Ticket } from "lucide-react";

interface VoucherTableProps {
  vouchers: VoucherResponse[];
}

export function VoucherTable({ vouchers }: VoucherTableProps) {
  console.log("Dữ liệu voucher:", vouchers);
  const { openDialog } = useVoucherDialogStore();

  const formatMoney = (amount: number) => {
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "—";
    const d = new Date(isoString);
    return d.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const getStatusBadge = (voucher: VoucherResponse) => {
    const isExpired = voucher.endDate && new Date(voucher.endDate) < new Date();
    if (!voucher.isActive) {
      return (
        <Badge variant="outline" className="bg-slate-100 text-slate-600 border-slate-300">
          Đã tắt
        </Badge>
      );
    }
    if (isExpired) {
      return (
        <Badge variant="outline" className="bg-amber-50 text-amber-700 border-amber-300">
          Hết hạn
        </Badge>
      );
    }
    return (
      <Badge variant="outline" className="bg-emerald-50 text-emerald-700 border-emerald-300">
        Đang hoạt động
      </Badge>
    );
  };

  if (!vouchers || vouchers.length === 0) {
    return (
      <div className="bg-white rounded-lg p-12 text-center border border-slate-200">
        <Ticket className="size-12 mx-auto text-slate-300 mb-3" />
        <h3 className="text-base font-semibold text-slate-700">Không tìm thấy mã giảm giá nào</h3>
        <p className="text-sm text-slate-500 mt-1">Thử thay đổi bộ lọc hoặc tạo mã giảm giá mới.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg border border-slate-200 overflow-hidden shadow-sm">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm text-slate-600">
          <thead className="bg-slate-50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200">
            <tr>
              <th className="px-4 py-3">Mã & Tên Voucher</th>
              <th className="px-4 py-3">Loại & Giá trị</th>
              <th className="px-4 py-3">Phạm vi</th>
              <th className="px-4 py-3">Lượt dùng</th>
              <th className="px-4 py-3">Thời gian hiệu lực</th>
              <th className="px-4 py-3">Trạng thái</th>
              <th className="px-4 py-3 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {vouchers.map((voucher) => (
              <tr key={voucher.code} className="hover:bg-slate-50/80 transition-colors">
                <td className="px-4 py-3.5">
                  <div className="flex flex-col gap-0.5">
                    <span className="font-mono font-bold text-blue-700 tracking-wider">
                      {voucher.code}
                    </span>
                    <span className="font-medium text-slate-900">{voucher.name}</span>
                    {voucher.description && (
                      <span className="text-xs text-slate-400 line-clamp-1">
                        {voucher.description}
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-4 py-3.5">
                  <div className="flex flex-col">
                    <span className="font-semibold text-slate-800">
                      {voucher.type === "PERCENTAGE"
                        ? `${voucher.value}%`
                        : formatMoney(voucher.value)}
                    </span>
                    {voucher.type === "PERCENTAGE" && voucher.maxDiscountAmount && (
                      <span className="text-xs text-slate-500">
                        Trần: {formatMoney(voucher.maxDiscountAmount)}
                      </span>
                    )}
                    {voucher.minOrderValue && (
                      <span className="text-xs text-slate-400">
                        Đơn tối thiểu: {formatMoney(voucher.minOrderValue)}
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-4 py-3.5">
                  {voucher.applyScope === "ALL" ? (
                    <Badge variant="secondary" className="bg-slate-100 text-slate-700">
                      Tất cả sản phẩm
                    </Badge>
                  ) : (
                    <Badge variant="secondary" className="bg-purple-50 text-purple-700">
                      {voucher.productSkus?.length || 0} SKU cụ thể
                    </Badge>
                  )}
                </td>
                <td className="px-4 py-3.5 font-medium text-slate-700">
                  {voucher.usedCount ?? 0} / {voucher.maxUses ?? "∞"}
                </td>
                <td className="px-4 py-3.5 text-xs text-slate-600">
                  <div>Từ: {formatDate(voucher.startDate)}</div>
                  <div>Đến: {formatDate(voucher.endDate)}</div>
                </td>
                <td className="px-4 py-3.5">{getStatusBadge(voucher)}</td>
                <td className="px-4 py-3.5 text-right">
                  <div className="flex items-center justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-8 text-slate-600 hover:text-blue-600 hover:bg-blue-50"
                      title="Chỉnh sửa"
                      onClick={() => openDialog("edit", voucher)}
                    >
                      <Edit2 className="size-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className={`size-8 ${
                        voucher.isActive
                          ? "text-slate-600 hover:text-amber-600 hover:bg-amber-50"
                          : "text-slate-600 hover:text-emerald-600 hover:bg-emerald-50"
                      }`}
                      title={voucher.isActive ? "Ngừng kích hoạt" : "Kích hoạt lại"}
                      onClick={() => openDialog("toggle-active", voucher)}
                    >
                      <Power className="size-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
