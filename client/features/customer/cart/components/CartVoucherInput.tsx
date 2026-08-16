"use client";

import React, { useState } from "react";
import { Ticket, Check, X, Tag, ChevronRight, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/shared/utils";
import { CartVoucherDrawer } from "./CartVoucherDrawer";

interface CartVoucherInputProps {
  subtotal: number;
  selectedSkuIds: number[];
  appliedVoucherCode: string | null;
  discountAmount: number;
  onApplyVoucher: (code: string) => Promise<{ success: boolean; message?: string }>;
  onRemoveVoucher: () => void;
}

export function CartVoucherInput({
  subtotal,
  selectedSkuIds,
  appliedVoucherCode,
  discountAmount,
  onApplyVoucher,
  onRemoveVoucher,
}: CartVoucherInputProps) {
  const [inputCode, setInputCode] = useState("");
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleApply = async (codeToApply: string) => {
    if (!codeToApply.trim()) return;
    setErrorMsg(null);
    setIsLoading(true);
    try {
      const res = await onApplyVoucher(codeToApply.trim().toUpperCase());
      if (!res.success) {
        setErrorMsg(res.message || "Mã giảm giá không hợp lệ hoặc chưa đủ điều kiện.");
      } else {
        setInputCode("");
      }
    } catch {
      setErrorMsg("Không thể áp dụng mã voucher lúc này. Vui lòng thử lại.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-3 pt-3 border-t border-slate-100">
      <div className="flex items-center justify-between">
        <label className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
          <Tag className="w-3.5 h-3.5 text-shop_light_green" />
          <span>Mã ưu đãi / Voucher</span>
        </label>
        <button
          type="button"
          onClick={() => setIsDrawerOpen(true)}
          className="text-xs font-bold text-shop_light_green hover:text-shop_dark_green flex items-center gap-0.5 transition-colors"
        >
          <span>Xem kho voucher</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {appliedVoucherCode ? (
        <div className="flex items-center justify-between p-3 rounded-xl bg-emerald-50/80 border border-emerald-200">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-emerald-600 text-white flex items-center justify-center flex-shrink-0">
              <Check className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-black text-emerald-900 tracking-wide uppercase">
                  {appliedVoucherCode}
                </span>
                <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-emerald-200/70 text-emerald-800">
                  Đã áp dụng
                </span>
              </div>
              <p className="text-[11px] font-semibold text-emerald-700">
                Tiết kiệm được: {formatCurrency(discountAmount)}
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onRemoveVoucher}
            className="h-8 px-2 text-xs font-semibold text-rose-600 hover:text-rose-700 hover:bg-rose-50 rounded-lg"
          >
            <X className="w-4 h-4 mr-1" />
            <span>Gỡ mã</span>
          </Button>
        </div>
      ) : (
        <div className="space-y-2">
          <div className="flex gap-2">
            <Input
              placeholder="Nhập mã voucher..."
              value={inputCode}
              onChange={(e) => {
                setInputCode(e.target.value.toUpperCase());
                if (errorMsg) setErrorMsg(null);
              }}
              onKeyDown={(e) => e.key === "Enter" && handleApply(inputCode)}
              className="h-10 text-xs font-semibold uppercase bg-slate-50 border-slate-200 focus:bg-white"
            />
            <Button
              type="button"
              disabled={!inputCode.trim() || isLoading}
              onClick={() => handleApply(inputCode)}
              className="h-10 px-4 text-xs font-bold bg-slate-900 hover:bg-slate-800 text-white rounded-xl flex-shrink-0"
            >
              {isLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : "Áp dụng"}
            </Button>
          </div>

          {errorMsg && (
            <p className="text-xs font-medium text-rose-500 bg-rose-50 p-2 rounded-lg border border-rose-100 animate-in fade-in-50">
              {errorMsg}
            </p>
          )}
        </div>
      )}

      {/* Slide-over Voucher Drawer */}
      <CartVoucherDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        subtotal={subtotal}
        selectedSkuIds={selectedSkuIds}
        appliedVoucherCode={appliedVoucherCode}
        onApplyVoucher={(code) => handleApply(code)}
        onRemoveVoucher={onRemoveVoucher}
      />
    </div>
  );
}
