"use client";

import React, { useState, useEffect, useMemo } from "react";
import { Ticket, Search, Check, AlertCircle, ShoppingBag, Sparkles, X, ArrowRight } from "lucide-react";
import { formatCurrency } from "@/shared/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet";
import { getVouchers } from "@/features/vouchers/services/voucher.service";
import { VoucherResponse } from "@/features/vouchers/voucher.type";

interface CartVoucherDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  subtotal: number;
  selectedSkuIds: number[];
  appliedVoucherCode: string | null;
  onApplyVoucher: (code: string) => void;
  onRemoveVoucher: () => void;
}

export function CartVoucherDrawer({
  isOpen,
  onClose,
  subtotal,
  selectedSkuIds,
  appliedVoucherCode,
  onApplyVoucher,
  onRemoveVoucher,
}: CartVoucherDrawerProps) {
  const [vouchers, setVouchers] = useState<VoucherResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [manualCode, setManualCode] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [activeTab, setActiveTab] = useState<"ALL" | "ELIGIBLE" | "INELIGIBLE">("ALL");

  useEffect(() => {
    if (isOpen) {
      setIsLoading(true);
      getVouchers({ active: true, status: "active", size: 50 })
        .then((res) => {
          if (res?.content) setVouchers(res.content);
        })
        .catch((err) => console.error("Lỗi tải voucher:", err))
        .finally(() => setIsLoading(false));
    }
  }, [isOpen]);

  const voucherEvaluation = useMemo(() => {
    return vouchers.map((v) => {
      const isExpired = Boolean(v.endDate && new Date(v.endDate) < new Date());
      const minOrder = v.minOrderValue || 0;
      const meetsMinOrder = subtotal >= minOrder;

      let meetsScope = true;
      if (v.applyScope === "SPECIFIC") {
        const eligibleSkuIds = new Set((v.productSkus || []).map((s) => s.id));
        meetsScope = selectedSkuIds.some((id) => eligibleSkuIds.has(id));
      }

      const isEligible = !isExpired && meetsMinOrder && meetsScope;

      let reason = "";
      if (isExpired) {
        reason = "Mã voucher đã hết hạn";
      } else if (!meetsMinOrder) {
        const diff = minOrder - subtotal;
        reason = `Mua thêm ${formatCurrency(diff)} để áp dụng mã này`;
      } else if (!meetsScope) {
        reason = "Giỏ hàng không chứa sản phẩm thuộc danh sách áp dụng";
      }

      return { voucher: v, isEligible, reason };
    });
  }, [vouchers, subtotal, selectedSkuIds]);

  const filteredList = useMemo(() => {
    return voucherEvaluation.filter(({ voucher, isEligible }) => {
      const matchesSearch =
        searchQuery.trim() === "" ||
        voucher.code.toLowerCase().includes(searchQuery.toLowerCase().trim()) ||
        voucher.name.toLowerCase().includes(searchQuery.toLowerCase().trim());

      if (!matchesSearch) return false;

      if (activeTab === "ELIGIBLE") return isEligible;
      if (activeTab === "INELIGIBLE") return !isEligible;
      return true;
    });
  }, [voucherEvaluation, searchQuery, activeTab]);

  const handleManualApply = () => {
    if (!manualCode.trim()) return;
    onApplyVoucher(manualCode.trim().toUpperCase());
    setManualCode("");
    onClose();
  };

  const handlePickVoucher = (code: string) => {
    onApplyVoucher(code);
    onClose();
  };

  return (
    <Sheet open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <SheetContent side="right" className="w-full sm:max-w-lg md:max-w-xl flex flex-col p-0 h-full">
        {/* Header */}
        <SheetHeader className="p-5 pb-4 border-b border-slate-100 bg-slate-900 text-white">
          <div className="flex items-center gap-2 text-emerald-400">
            <Sparkles className="w-5 h-5" />
            <SheetTitle className="text-lg font-bold text-white">Kho Voucher Ưu Đãi</SheetTitle>
          </div>
          <SheetDescription className="text-xs text-slate-300">
            Chọn hoặc nhập mã giảm giá để nhận mức giá ưu đãi tốt nhất cho đơn hàng của bạn.
          </SheetDescription>
        </SheetHeader>

        {/* Quick Manual Code Input */}
        <div className="p-4 bg-slate-50 border-b border-slate-200/80">
          <p className="text-xs font-bold text-slate-700 mb-2">Nhập mã voucher trực tiếp:</p>
          <div className="flex gap-2">
            <Input
              placeholder="VD: GIAM10, FREESHIP..."
              value={manualCode}
              onChange={(e) => setManualCode(e.target.value.toUpperCase())}
              onKeyDown={(e) => e.key === "Enter" && handleManualApply()}
              className="h-10 text-xs font-bold uppercase bg-white border-slate-200"
            />
            <Button
              type="button"
              disabled={!manualCode.trim()}
              onClick={handleManualApply}
              className="h-10 px-4 text-xs font-bold bg-slate-900 hover:bg-slate-800 text-white rounded-xl"
            >
              Áp dụng
            </Button>
          </div>
        </div>

        {/* Subtotal Indicator & Tabs */}
        <div className="px-5 pt-3 pb-2 border-b border-slate-100 space-y-3">
          <div className="flex justify-between items-center text-xs">
            <span className="text-slate-500">Tạm tính giỏ hàng:</span>
            <span className="font-bold text-shop_dark_green text-sm">{formatCurrency(subtotal)}</span>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
              <Input
                placeholder="Tìm mã voucher..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 h-8 text-xs bg-slate-50 border-slate-200"
              />
            </div>
            <div className="flex gap-1">
              <button
                onClick={() => setActiveTab("ALL")}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
                  activeTab === "ALL" ? "bg-slate-900 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                Tất cả ({voucherEvaluation.length})
              </button>
              <button
                onClick={() => setActiveTab("ELIGIBLE")}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all ${
                  activeTab === "ELIGIBLE" ? "bg-emerald-600 text-white" : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                }`}
              >
                Khả dụng ({voucherEvaluation.filter((i) => i.isEligible).length})
              </button>
            </div>
          </div>
        </div>

        {/* Voucher List Scroll Area */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-3.5">
          {isLoading ? (
            <div className="py-12 text-center text-xs text-slate-400">Đang tải danh sách voucher...</div>
          ) : filteredList.length > 0 ? (
            filteredList.map(({ voucher, isEligible, reason }) => {
              const isApplied = appliedVoucherCode === voucher.code;
              const isPercentage = voucher.type === "PERCENTAGE";

              return (
                <div
                  key={voucher.code}
                  className={`rounded-2xl border transition-all p-4 ${
                    isApplied
                      ? "border-emerald-500 bg-emerald-50/40 ring-2 ring-emerald-500/20"
                      : isEligible
                      ? "border-slate-200 bg-white hover:border-shop_light_green/60 shadow-xs"
                      : "border-slate-200/60 bg-slate-50/70 opacity-60"
                  }`}
                >
                  <div className="flex justify-between items-start gap-2 mb-1.5">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-sm font-black text-slate-900 uppercase">
                          {voucher.code}
                        </span>
                        <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                          {voucher.applyScope === "ALL" ? "Toàn sàn" : "Chọn lọc"}
                        </Badge>
                      </div>
                      <p className="text-base font-black text-slate-900 mt-0.5">
                        {isPercentage ? `Giảm ${voucher.value}%` : `Giảm ${formatCurrency(voucher.value)}`}
                        {isPercentage && voucher.maxDiscountAmount && voucher.maxDiscountAmount > 0 && (
                          <span className="text-xs font-semibold text-slate-500 ml-1">
                            (Tối đa {formatCurrency(voucher.maxDiscountAmount)})
                          </span>
                        )}
                      </p>
                    </div>

                    {isEligible && (
                      <Button
                        size="sm"
                        onClick={() => handlePickVoucher(voucher.code)}
                        className={`text-xs font-bold px-3 py-1 rounded-xl h-8 transition-all ${
                          isApplied
                            ? "bg-emerald-600 hover:bg-emerald-700 text-white"
                            : "bg-shop_light_green hover:bg-shop_dark_green text-white"
                        }`}
                      >
                        {isApplied ? "Đang chọn" : "Áp dụng"}
                      </Button>
                    )}
                  </div>

                  <p className="text-xs text-slate-600 font-medium">{voucher.name}</p>

                  <div className="mt-2 text-[11px] text-slate-500 space-y-0.5">
                    <p>Đơn tối thiểu: {formatCurrency(voucher.minOrderValue || 0)}</p>
                    <p>HSD: {new Date(voucher.endDate).toLocaleDateString("vi-VN")}</p>
                  </div>

                  {!isEligible && reason && (
                    <div className="mt-2.5 flex items-center gap-1.5 text-xs font-semibold text-amber-700 bg-amber-50/80 p-2 rounded-lg border border-amber-200/60">
                      <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                      <span>{reason}</span>
                    </div>
                  )}
                </div>
              );
            })
          ) : (
            <div className="py-12 text-center">
              <Ticket className="w-10 h-10 mx-auto text-slate-300 mb-2" />
              <p className="text-xs font-bold text-slate-700">Không có mã voucher phù hợp</p>
            </div>
          )}
        </div>

        {/* Footer info */}
        {appliedVoucherCode && (
          <div className="p-4 border-t border-slate-200 bg-white flex justify-between items-center">
            <div className="text-xs">
              <span className="text-slate-500">Mã đang chọn: </span>
              <span className="font-bold text-emerald-600">{appliedVoucherCode}</span>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={onRemoveVoucher}
              className="text-xs font-semibold text-rose-600 border-rose-200 hover:bg-rose-50 rounded-lg h-8"
            >
              <X className="w-3.5 h-3.5 mr-1" />
              Gỡ voucher
            </Button>
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
