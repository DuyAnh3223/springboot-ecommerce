"use client";

import React, { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { ShieldCheck, Truck, RotateCcw, ArrowRight } from "lucide-react";
import { formatCurrency } from "@/shared/utils";
import { Button } from "@/components/ui/button";
import { CartVoucherInput } from "./CartVoucherInput";
import { getVoucher } from "@/features/vouchers/services/voucher.service";
import { VoucherResponse } from "@/features/vouchers/voucher.type";
import { buildCheckoutUrl } from "@/features/customer/checkout/utils/checkout.utils";

interface CartSummaryProps {
  selectedCount: number;
  selectedQuantity: number;
  subtotal: number;
  selectedSkuIds?: number[];
}

export function CartSummary({
  selectedCount,
  selectedQuantity,
  subtotal,
  selectedSkuIds = [],
}: CartSummaryProps) {
  const router = useRouter();
  const [appliedVoucher, setAppliedVoucher] = useState<VoucherResponse | null>(null);

  const FLAT_SHIPPING_FEE = subtotal > 0 ? 30000 : 0;
  const isCheckoutDisabled = selectedCount === 0 || selectedSkuIds.length === 0;

  const discountAmount = useMemo(() => {
    if (!appliedVoucher) {
      return 0;
    }

    const minOrder = appliedVoucher.minOrderValue || 0;
    if (subtotal < minOrder) {
      // Subtotal dropped below minOrderValue -> invalid
      return 0;
    }

    const eligibleSubtotal = subtotal;
    if (appliedVoucher.applyScope === "SPECIFIC") {
      const eligibleSkuSet = new Set((appliedVoucher.productSkus || []).map((s) => s.id));
      const hasMatch = selectedSkuIds.some((id) => eligibleSkuSet.has(id));
      if (!hasMatch) {
        return 0;
      }
    }

    let computedDiscount = 0;
    if (appliedVoucher.type === "FIXED_AMOUNT") {
      computedDiscount = Math.min(appliedVoucher.value, eligibleSubtotal);
    } else if (appliedVoucher.type === "PERCENTAGE") {
      computedDiscount = (eligibleSubtotal * appliedVoucher.value) / 100;
      if (appliedVoucher.maxDiscountAmount && appliedVoucher.maxDiscountAmount > 0) {
        computedDiscount = Math.min(computedDiscount, appliedVoucher.maxDiscountAmount);
      }
    }

    computedDiscount = Math.min(computedDiscount, eligibleSubtotal);
    return Math.round(computedDiscount);
  }, [appliedVoucher, subtotal, selectedSkuIds]);

  const handleApplyVoucher = async (code: string) => {
    try {
      const voucher = await getVoucher(code);
      if (!voucher || !voucher.isActive) {
        return { success: false, message: "Mã voucher không tồn tại hoặc đã hết hiệu lực." };
      }

      if (voucher.endDate && new Date(voucher.endDate) < new Date()) {
        return { success: false, message: "Mã voucher này đã hết hạn sử dụng." };
      }

      const minOrder = voucher.minOrderValue || 0;
      if (subtotal < minOrder) {
        return {
          success: false,
          message: `Đơn hàng chưa đạt giá trị tối thiểu ${formatCurrency(minOrder)} để áp dụng mã này.`,
        };
      }

      if (voucher.applyScope === "SPECIFIC") {
        const eligibleSkuSet = new Set((voucher.productSkus || []).map((s) => s.id));
        const hasMatch = selectedSkuIds.some((id) => eligibleSkuSet.has(id));
        if (!hasMatch) {
          return {
            success: false,
            message: "Mã giảm giá này chỉ áp dụng cho một số sản phẩm chỉ định chưa có trong danh sách chọn.",
          };
        }
      }

      setAppliedVoucher(voucher);
      return { success: true };
    } catch {
      return { success: false, message: "Không tìm thấy mã voucher hợp lệ." };
    }
  };

  const handleRemoveVoucher = () => {
    setAppliedVoucher(null);
  };

  const handleCheckout = () => {
    if (isCheckoutDisabled) return;

    router.push(buildCheckoutUrl(selectedSkuIds, appliedVoucher?.code));
  };

  const totalCheckout = Math.max(0, subtotal + FLAT_SHIPPING_FEE - discountAmount);

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

          <div className="flex justify-between text-slate-600">
            <span>Tạm tính hàng:</span>
            <span className="font-semibold text-slate-800">{formatCurrency(subtotal)}</span>
          </div>

          <div className="flex justify-between text-slate-600">
            <span>Phí vận chuyển tạm tính:</span>
            <span className="font-semibold text-slate-800">
              {subtotal > 0 ? formatCurrency(FLAT_SHIPPING_FEE) : "0đ"}
            </span>
          </div>

          {discountAmount > 0 && (
            <div className="flex justify-between text-emerald-600 font-semibold">
              <span>Giảm giá voucher:</span>
              <span>-{formatCurrency(discountAmount)}</span>
            </div>
          )}

          <div className="flex justify-between items-baseline pt-3 border-t border-slate-100">
            <span className="text-base font-bold text-slate-800">Tổng thanh toán:</span>
            <span className="text-xl font-black text-shop_dark_green">
              {formatCurrency(totalCheckout)}
            </span>
          </div>
        </div>

        {/* Voucher Section */}
        <CartVoucherInput
          subtotal={subtotal}
          selectedSkuIds={selectedSkuIds}
          appliedVoucherCode={appliedVoucher?.code || null}
          discountAmount={discountAmount}
          onApplyVoucher={handleApplyVoucher}
          onRemoveVoucher={handleRemoveVoucher}
        />

        <div className="pt-2">
          <Button
            type="button"
            disabled={isCheckoutDisabled}
            onClick={handleCheckout}
            className="w-full h-12 bg-shop_light_green hover:bg-shop_dark_green text-white font-bold rounded-xl shadow-sm text-base transition-all disabled:opacity-50"
          >
            {isCheckoutDisabled ? (
              "Vui lòng chọn sản phẩm"
            ) : (
              <span className="flex items-center justify-center">
                Tiến hành thanh toán <ArrowRight className="ml-2 h-4 w-4" />
              </span>
            )}
          </Button>
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
