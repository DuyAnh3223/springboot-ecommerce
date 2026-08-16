"use client";

import React, { useState } from "react";
import Link from "next/link";
import { Copy, Check, ChevronDown, ChevronUp, TicketPercent, Calendar, ShoppingBag, Info, PackageCheck } from "lucide-react";
import { formatCurrency } from "@/shared/utils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CustomerVoucherCardProps } from "../types/customer-voucher.types";

export function CustomerVoucherCard({ voucher, onApply, isApplied, disabled, disabledReason }: CustomerVoucherCardProps) {
  const [isCopied, setIsCopied] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(voucher.code);
    setIsCopied(true);
    setTimeout(() => setIsCopied(false), 2000);
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return "—";
    const d = new Date(isoString);
    return d.toLocaleDateString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const isExpired = Boolean(voucher.endDate && new Date(voucher.endDate) < new Date());
  const isPercentage = voucher.type === "PERCENTAGE";

  return (
    <div className={`relative flex flex-col rounded-2xl border transition-all duration-200 bg-white overflow-hidden shadow-xs hover:shadow-md ${
      disabled ? "border-slate-200 opacity-60 bg-slate-50/50" : isApplied ? "border-shop_light_green ring-2 ring-shop_light_green/20" : "border-slate-200/80 hover:border-shop_light_green/50"
    }`}>
      {/* Top Banner Ribbon */}
      <div className="flex items-center justify-between px-4 py-2 bg-gradient-to-r from-slate-900 to-slate-800 text-white text-xs">
        <div className="flex items-center gap-1.5 font-bold tracking-wider uppercase text-emerald-400">
          <TicketPercent className="w-3.5 h-3.5" />
          <span>{voucher.code}</span>
        </div>
        <Badge variant="outline" className="text-[10px] px-2 py-0.5 border-white/20 text-slate-200">
          {voucher.applyScope === "ALL" ? "Toàn sàn" : "Sản phẩm chọn lọc"}
        </Badge>
      </div>

      {/* Main Ticket Content */}
      <div className="p-4 sm:p-5 flex-1 flex flex-col justify-between gap-4">
        <div>
          <div className="flex items-baseline gap-2 mb-1">
            <span className="text-xl sm:text-2xl font-black text-slate-900">
              {isPercentage ? `Giảm ${voucher.value}%` : `Giảm ${formatCurrency(voucher.value)}`}
            </span>
            {isPercentage && voucher.maxDiscountAmount && voucher.maxDiscountAmount > 0 && (
              <span className="text-xs font-semibold text-slate-500">
                (Tối đa {formatCurrency(voucher.maxDiscountAmount)})
              </span>
            )}
          </div>

          <h3 className="text-sm font-bold text-slate-800 line-clamp-1">{voucher.name}</h3>

          {voucher.description && (
            <p className="text-xs text-slate-500 mt-1 line-clamp-2">{voucher.description}</p>
          )}

          <div className="mt-3 space-y-1 text-xs text-slate-600">
            <div className="flex items-center gap-1.5 font-medium">
              <ShoppingBag className="w-3.5 h-3.5 text-shop_light_green flex-shrink-0" />
              <span>Đơn tối thiểu: {voucher.minOrderValue ? formatCurrency(voucher.minOrderValue) : "0đ"}</span>
            </div>
            <div className="flex items-center gap-1.5 text-slate-500">
              <Calendar className="w-3.5 h-3.5 flex-shrink-0" />
              <span>HSD: {formatDate(voucher.endDate)}</span>
            </div>
          </div>

          {disabledReason && (
            <p className="mt-2 text-xs font-semibold text-rose-500 bg-rose-50 p-2 rounded-lg border border-rose-100">
              {disabledReason}
            </p>
          )}
        </div>

        {/* Action Buttons */}
        <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
          <button
            type="button"
            onClick={() => setIsExpanded(!isExpanded)}
            className="text-xs font-semibold text-slate-500 hover:text-shop_light_green flex items-center gap-1 transition-colors"
          >
            <span>Chi tiết điều kiện</span>
            {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>

          <div className="flex items-center gap-2">
            {onApply ? (
              <Button
                size="sm"
                disabled={disabled || isExpired}
                onClick={() => onApply(voucher.code)}
                className={`text-xs font-bold px-3 py-1.5 rounded-lg transition-all ${
                  isApplied ? "bg-emerald-600 hover:bg-emerald-700 text-white" : "bg-shop_light_green hover:bg-shop_dark_green text-white"
                }`}
              >
                {isApplied ? "Đang chọn" : "Áp dụng"}
              </Button>
            ) : (
              <>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleCopy}
                  className="text-xs font-medium px-2.5 py-1.5 border-slate-200 hover:bg-slate-50 text-slate-700"
                >
                  {isCopied ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-600 mr-1" />
                      <span className="text-emerald-600 font-bold">Đã chép</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5 mr-1" />
                      <span>Sao chép</span>
                    </>
                  )}
                </Button>
                <Link href="/category">
                  <Button size="sm" className="text-xs font-bold px-3 py-1.5 bg-shop_light_green hover:bg-shop_dark_green text-white">
                    Dùng ngay
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </div>

      {/* In-place Expandable Detail Panel */}
      {isExpanded && (
        <div className="px-4 pb-4 pt-2 bg-slate-50/80 border-t border-slate-100 text-xs text-slate-600 space-y-2 animate-in fade-in-50 duration-150">
          <div className="flex items-start gap-1.5">
            <Info className="w-3.5 h-3.5 text-shop_light_green flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-slate-800">Quy định áp dụng:</p>
              <ul className="list-disc list-inside mt-1 space-y-0.5 text-slate-500">
                <li>Hiệu lực: {formatDate(voucher.startDate)} đến {formatDate(voucher.endDate)}</li>
                {voucher.maxPerUser && <li>Mỗi tài khoản được sử dụng tối đa {voucher.maxPerUser} lần.</li>}
                {voucher.maxUses && <li>Số lượt dùng tối đa toàn hệ thống: {voucher.maxUses} lượt.</li>}
                <li>Giảm {isPercentage ? `${voucher.value}%` : formatCurrency(voucher.value)} trên các sản phẩm hợp lệ.</li>
              </ul>
            </div>
          </div>

          {voucher.applyScope === "SPECIFIC" && voucher.productSkus && voucher.productSkus.length > 0 && (
            <div className="pt-2 border-t border-slate-200">
              <p className="font-semibold text-slate-800 flex items-center gap-1 mb-1.5">
                <PackageCheck className="w-3.5 h-3.5 text-shop_light_green" />
                <span>Sản phẩm áp dụng ({voucher.productSkus.length} SKU):</span>
              </p>
              <div className="max-h-28 overflow-y-auto space-y-1 pr-1">
                {voucher.productSkus.map((sku) => (
                  <div key={sku.id} className="flex justify-between items-center bg-white p-1.5 rounded border border-slate-200">
                    <span className="font-medium text-slate-700 truncate mr-2">{sku.productName || sku.sku}</span>
                    <span className="font-bold text-shop_dark_green whitespace-nowrap">{formatCurrency(sku.price)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
