import React, { useEffect, useRef, useState } from "react";
import { getSkusAction } from "@/features/skus/actions/get-skus.action";
import { SkuResponse } from "@/features/skus/sku.type";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Loader2, Search, ChevronLeft, ChevronRight, PackageCheck } from "lucide-react";
import { useAsyncAction } from "@/shared/hooks/useAsyncAction";

interface SkuSelectorModalProps {
  open: boolean;
  onClose: () => void;
  initialSelectedSkuIds: number[];
  onConfirm: (selectedSkuIds: number[]) => void;
}

export function SkuSelectorModal({
  open,
  onClose,
  initialSelectedSkuIds,
  onConfirm,
}: SkuSelectorModalProps) {
  const [localSelectedIds, setLocalSelectedIds] = useState<Set<number>>(
    () => new Set(initialSelectedSkuIds),
  );
  const [skuSearch, setSkuSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const searchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [page, setPage] = useState(1);
  const pageSize = 10;
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [skusList, setSkusList] = useState<SkuResponse[]>([]);
  const { isLoading, run } = useAsyncAction();

  const handleSearchChange = (value: string) => {
    setSkuSearch(value);
    setPage(1);
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    searchTimerRef.current = setTimeout(() => setDebouncedSearch(value), 400);
  };

  // Fetch SKUs from API
  useEffect(() => {
    if (!open) return;

    let cancelled = false;
    void run(async () => {
      const result = await getSkusAction({ search: debouncedSearch, page, size: pageSize });
      if (cancelled) return;
      if (result.skus) {
        setSkusList(result.skus.content || []);
        setTotalPages(result.skus.totalPages || 1);
        setTotalElements(result.skus.totalElements || 0);
      } else {
        setSkusList([]);
        setTotalPages(1);
        setTotalElements(0);
      }
    });

    return () => {
      cancelled = true;
    };
  }, [open, debouncedSearch, page, run]);

  const toggleSku = (id: number) => {
    const updated = new Set(localSelectedIds);
    if (updated.has(id)) {
      updated.delete(id);
    } else {
      updated.add(id);
    }
    setLocalSelectedIds(updated);
  };

  const handleConfirm = () => {
    onConfirm(Array.from(localSelectedIds));
    onClose();
  };

  const formatMoney = (amount: number) =>
    new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(amount);

  return (
    <Dialog open={open} onOpenChange={(state) => !state && onClose()}>
      <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-slate-800">
            <PackageCheck className="size-5 text-blue-600" />
            Chọn sản phẩm (SKU) áp dụng cho Voucher
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-3 flex-1 overflow-hidden flex flex-col pt-1">
          {/* Search bar */}
          <div className="relative">
            <Search className="size-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <Input
              type="text"
              placeholder="Tra cứu mã SKU hoặc tên sản phẩm..."
              value={skuSearch}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="pl-9 h-10 text-sm bg-white"
            />
          </div>

          {/* SKU Table / List */}
          <div className="flex-1 overflow-y-auto border border-slate-200 rounded-md bg-white">
            {isLoading ? (
              <div className="flex items-center justify-center p-12 text-sm text-slate-500">
                <Loader2 className="size-5 animate-spin mr-2 text-blue-600" />
                Đang tải danh sách SKU sản phẩm...
              </div>
            ) : skusList.length === 0 ? (
              <div className="p-8 text-center text-sm text-slate-500">
                Không tìm thấy SKU nào khớp với từ khóa tìm kiếm.
              </div>
            ) : (
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200 sticky top-0">
                  <tr>
                    <th className="px-3 py-2.5 w-10 text-center">Chọn</th>
                    <th className="px-3 py-2.5">Mã SKU</th>
                    <th className="px-3 py-2.5">Tên sản phẩm</th>
                    <th className="px-3 py-2.5 text-right">Đơn giá</th>
                    <th className="px-3 py-2.5 text-center">Tồn kho</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {skusList.map((sku) => {
                    const isChecked = localSelectedIds.has(sku.id);
                    return (
                      <tr
                        key={sku.id}
                        onClick={() => toggleSku(sku.id)}
                        className={`cursor-pointer transition-colors ${
                          isChecked ? "bg-blue-50/60" : "hover:bg-slate-50"
                        }`}
                      >
                        <td className="px-3 py-2.5 text-center" onClick={(e) => e.stopPropagation()}>
                          <Checkbox
                            checked={isChecked}
                            onCheckedChange={() => toggleSku(sku.id)}
                          />
                        </td>
                        <td className="px-3 py-2.5 font-mono font-bold text-slate-800">
                          {sku.sku}
                        </td>
                        <td className="px-3 py-2.5 text-slate-600 font-medium line-clamp-1">
                          {sku.productName || "Sản phẩm không có tên"}
                        </td>
                        <td className="px-3 py-2.5 text-right font-semibold text-slate-800">
                          {formatMoney(sku.price)}
                        </td>
                        <td className="px-3 py-2.5 text-center font-medium text-slate-700">
                          {sku.stock}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>

          {/* Pagination bar */}
          <div className="flex items-center justify-between text-xs text-slate-500 pt-1">
            <span>
              Tổng số <strong className="text-slate-800">{totalElements}</strong> SKU
            </span>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={page <= 1 || isLoading}
                onClick={() => setPage((p) => Math.max(p - 1, 1))}
                className="h-8 px-2 text-xs"
              >
                <ChevronLeft className="size-3.5 mr-1" /> Trước
              </Button>
              <span className="font-medium text-slate-700">
                Trang {page} / {totalPages}
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={page >= totalPages || isLoading}
                onClick={() => setPage((p) => Math.min(p + 1, totalPages))}
                className="h-8 px-2 text-xs"
              >
                Sau <ChevronRight className="size-3.5 ml-1" />
              </Button>
            </div>
          </div>
        </div>

        <DialogFooter className="pt-3 border-t border-slate-100 flex items-center justify-between">
          <div className="text-xs text-slate-600">
            Đã chọn: <strong className="text-blue-700">{localSelectedIds.size}</strong> SKU
          </div>
          <div className="flex gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Hủy
            </Button>
            <Button type="button" onClick={handleConfirm} className="bg-blue-600 hover:bg-blue-700 text-white">
              Xác nhận ({localSelectedIds.size})
            </Button>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
