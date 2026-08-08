"use client";

import React from "react";
import { ShieldCheck, Truck, RotateCcw, ArrowRight } from "lucide-react";
import { formatCurrency } from "@/shared/utils";
import { Button } from "@/components/ui/button";

interface CartSummaryProps {
  selectedCount: number;
  selectedQuantity: number;
  subtotal: number;
}

export function CartSummary({
  selectedCount,
  selectedQuantity,
  subtotal,
}: CartSummaryProps) {
  const isCheckoutDisabled = selectedCount === 0;

  return (
    <div className="space-y-4">
      <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-xs md:p-6 space-y-5">
        <h2 className="text-lg font-bold text-slate-800 border-b border-slate-100 pb-3">
          Tóm Tắt Đơn Hàng
        </h2>

        <div className="space-y-3 text-sm">
          <div className="flex justify-between text-slate-600">
            <span>Sản phẩm đã chọn:</span>
            <span className="font-semibold text-slate-800">
              {selectedQuantity} sản phẩm ({selectedCount} loại)
            </span>
          </div>

          <div className="flex justify-between items-baseline pt-2 border-t border-slate-100">
            <span className="text-base font-bold text-slate-800">
              Tạm tính sản phẩm đang chọn:
            </span>
            <span className="text-xl font-black text-shop_dark_green">
              {formatCurrency(subtotal)}
            </span>
          </div>
        </div>

        <div className="pt-2">
          <Button
            disabled={isCheckoutDisabled}
            className="w-full h-12 bg-shop_light_green hover:bg-shop_dark_green text-white font-bold rounded-xl shadow-sm text-base transition-all disabled:opacity-50"
          >
            {isCheckoutDisabled ? (
              "Vui lòng chọn sản phẩm"
            ) : (
              <span className="flex items-center justify-center">
                Tiến hành thanh toán (Sắp ra mắt) <ArrowRight className="ml-2 h-4 w-4" />
              </span>
            )}
          </Button>
          <p className="mt-2 text-center text-xs text-slate-400">
            Tính năng thanh toán sẽ được cập nhật sau.
          </p>
        </div>
      </div>

      {/* Trust Badges */}
      <div className="rounded-2xl border border-slate-100 bg-slate-50/50 p-4 space-y-3">
        <div className="flex items-center space-x-3 text-xs text-slate-600">
          <Truck className="h-4 w-4 text-shop_light_green flex-shrink-0" />
          <span>Giao hàng nhanh toàn quốc</span>
        </div>
        <div className="flex items-center space-x-3 text-xs text-slate-600">
          <ShieldCheck className="h-4 w-4 text-shop_light_green flex-shrink-0" />
          <span>Bảo hành chính hãng 100%</span>
        </div>
        <div className="flex items-center space-x-3 text-xs text-slate-600">
          <RotateCcw className="h-4 w-4 text-shop_light_green flex-shrink-0" />
          <span>Đổi trả dễ dàng trong vòng 30 ngày</span>
        </div>
      </div>
    </div>
  );
}
