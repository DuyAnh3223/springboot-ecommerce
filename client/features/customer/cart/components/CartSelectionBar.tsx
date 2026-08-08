"use client";

import React, { useState } from "react";
import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";

interface CartSelectionBarProps {
  totalCount: number;
  isAllSelected: boolean;
  onToggleAll: (selected: boolean) => void;
  onClearCart: () => void;
  isPending?: boolean;
  isClearPending?: boolean;
}

export function CartSelectionBar({
  totalCount,
  isAllSelected,
  onToggleAll,
  onClearCart,
  isPending = false,
  isClearPending = false,
}: CartSelectionBarProps) {
  const [showConfirm, setShowConfirm] = useState(false);
  const isDisabled = isPending || isClearPending || totalCount === 0;

  const handleClearClick = () => {
    onClearCart();
    setShowConfirm(false);
  };

  return (
    <div className="flex items-center justify-between rounded-xl border border-slate-100 bg-white p-4 shadow-2xs">
      <label className="flex items-center space-x-3 cursor-pointer select-none">
        <input
          type="checkbox"
          checked={isAllSelected}
          onChange={(e) => onToggleAll(e.target.checked)}
          className="h-4 w-4 rounded border-slate-300 text-shop_light_green focus:ring-shop_light_green cursor-pointer"
        />
        <span className="text-sm font-semibold text-slate-700">
          Chọn tất cả ({totalCount} sản phẩm)
        </span>
      </label>

      {showConfirm ? (
        <div className="flex items-center space-x-2">
          <span className="text-xs text-red-600 font-medium">Xác nhận xóa hết?</span>
          <Button
            size="sm"
            variant="destructive"
            onClick={handleClearClick}
            disabled={isDisabled}
            className="h-7 text-xs px-2.5 rounded-lg"
          >
            {isClearPending ? "Đang xóa..." : "Có, xóa"}
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => setShowConfirm(false)}
            disabled={isClearPending}
            className="h-7 text-xs px-2.5 rounded-lg border-slate-200"
          >
            Hủy
          </Button>
        </div>
      ) : (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          disabled={isDisabled}
          onClick={() => setShowConfirm(true)}
          className="h-8 text-xs text-slate-500 hover:bg-red-50 hover:text-red-600 rounded-lg transition-colors"
        >
          <Trash2 className="mr-1.5 h-3.5 w-3.5" /> Xóa tất cả giỏ hàng
        </Button>
      )}
    </div>
  );
}
