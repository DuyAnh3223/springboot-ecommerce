"use client";

import React, { useState } from "react";
import { SellingMode } from "../../types/sku.draft.type";
import { Package, Layers, AlertTriangle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";

interface SellingModeSelectorProps {
  mode: SellingMode;
  hasVariantAttributes: boolean;
  onModeChange: (newMode: SellingMode) => void;
  hasExistingData: boolean;
}

export function SellingModeSelector({
  mode,
  hasVariantAttributes,
  onModeChange,
  hasExistingData,
}: SellingModeSelectorProps) {
  const [pendingMode, setPendingMode] = useState<SellingMode | null>(null);

  const handleSelectMode = (targetMode: SellingMode) => {
    if (targetMode === mode) return;

    if (hasExistingData) {
      setPendingMode(targetMode);
    } else {
      onModeChange(targetMode);
    }
  };

  const confirmModeSwitch = () => {
    if (pendingMode) {
      onModeChange(pendingMode);
      setPendingMode(null);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 rounded-2xl bg-slate-50 border border-slate-200">
        <div>
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            Mô hình bán hàng
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Chọn mô hình phù hợp để quản lý giá bán, mã SKU và tồn kho sản phẩm.
          </p>
        </div>

        <div className="inline-flex p-1 rounded-xl bg-slate-200/80 border border-slate-200">
          <button
            type="button"
            onClick={() => handleSelectMode("single")}
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-bold transition-all ${
              mode === "single"
                ? "bg-white text-slate-900 shadow-xs"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <Package className="w-4 h-4 text-emerald-600" />
            <span>Sản phẩm đơn</span>
          </button>

          <button
            type="button"
            disabled={!hasVariantAttributes}
            onClick={() => handleSelectMode("multi")}
            title={
              !hasVariantAttributes
                ? "Danh mục này chưa được cấu hình thuộc tính biến thể"
                : ""
            }
            className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-bold transition-all ${
              mode === "multi"
                ? "bg-white text-slate-900 shadow-xs"
                : "text-slate-600 hover:text-slate-900 disabled:opacity-50 disabled:cursor-not-allowed"
            }`}
          >
            <Layers className="w-4 h-4 text-indigo-600" />
            <span>Sản phẩm có biến thể</span>
          </button>
        </div>
      </div>

      {!hasVariantAttributes && (
        <p className="text-[11px] text-amber-600 font-medium flex items-center gap-1">
          <AlertTriangle className="w-3.5 h-3.5" />
          <span>Danh mục hiện tại không có thuộc tính phân loại biến thể. Sản phẩm sẽ được khởi tạo dưới dạng sản phẩm đơn.</span>
        </p>
      )}

      {/* Confirmation Dialog */}
      <Dialog open={!!pendingMode} onOpenChange={() => setPendingMode(null)}>
        <DialogContent className="sm:max-w-md bg-white rounded-2xl">
          <DialogHeader>
            <DialogTitle className="text-base font-bold text-slate-900 flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-amber-500" />
              Xác nhận chuyển đổi mô hình
            </DialogTitle>
            <DialogDescription className="text-xs text-slate-600 pt-2">
              Bạn đang chuyển từ <strong>{mode === "single" ? "Sản phẩm đơn" : "Sản phẩm biến thể"}</strong> sang{" "}
              <strong>{pendingMode === "single" ? "Sản phẩm đơn" : "Sản phẩm biến thể"}</strong>.
              <br />
              Việc này có thể làm thay đổi hoặc sinh lại danh sách SKU. Các SKU không thuộc cấu hình mới sẽ được lưu trữ an toàn.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:gap-0 pt-3">
            <Button variant="outline" size="sm" onClick={() => setPendingMode(null)}>
              Hủy Bỏ
            </Button>
            <Button
              size="sm"
              className="bg-rose-600 hover:bg-rose-700 text-white font-bold"
              onClick={confirmModeSwitch}
            >
              Xác Nhận Chuyển
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
