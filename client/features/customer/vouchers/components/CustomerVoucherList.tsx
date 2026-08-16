"use client";

import React, { useState, useMemo } from "react";
import { Search, Ticket, RefreshCw } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { CustomerVoucherCard } from "./CustomerVoucherCard";
import { VoucherResponse } from "@/features/vouchers/voucher.type";
import { CustomerVoucherFilterTab } from "../types/customer-voucher.types";

interface CustomerVoucherListProps {
  initialVouchers: VoucherResponse[];
}

export function CustomerVoucherList({ initialVouchers }: CustomerVoucherListProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [activeTab, setActiveTab] = useState<CustomerVoucherFilterTab>("ALL");

  const filteredVouchers = useMemo(() => {
    return initialVouchers.filter((v) => {
      const matchesSearch =
        searchTerm.trim() === "" ||
        v.code.toLowerCase().includes(searchTerm.toLowerCase().trim()) ||
        v.name.toLowerCase().includes(searchTerm.toLowerCase().trim());

      if (!matchesSearch) return false;

      switch (activeTab) {
        case "PERCENTAGE":
          return v.type === "PERCENTAGE";
        case "FIXED_AMOUNT":
          return v.type === "FIXED_AMOUNT";
        case "SCOPE_ALL":
          return v.applyScope === "ALL";
        case "SCOPE_SPECIFIC":
          return v.applyScope === "SPECIFIC";
        default:
          return true;
      }
    });
  }, [initialVouchers, searchTerm, activeTab]);

  const tabs: { id: CustomerVoucherFilterTab; label: string }[] = [
    { id: "ALL", label: "Tất cả" },
    { id: "PERCENTAGE", label: "Giảm theo %" },
    { id: "FIXED_AMOUNT", label: "Giảm tiền mặt" },
    { id: "SCOPE_ALL", label: "Toàn bộ sàn" },
    { id: "SCOPE_SPECIFIC", label: "Sản phẩm chỉ định" },
  ];

  return (
    <div className="space-y-6">
      {/* Controls: Search & Tabs */}
      <div className="flex flex-col md:flex-row gap-4 justify-between items-stretch md:items-center bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <Input
            placeholder="Tìm theo mã hoặc tên voucher..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 h-10 rounded-xl bg-slate-50 border-slate-200 focus:bg-white text-sm"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0 scrollbar-none">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${
                activeTab === tab.id
                  ? "bg-slate-900 text-white shadow-xs"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200/70"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Counter */}
      <div className="flex items-center justify-between text-xs text-slate-500 px-1">
        <span className="font-semibold text-slate-700">
          Tìm thấy <span className="text-shop_light_green font-bold">{filteredVouchers.length}</span> mã ưu đãi
        </span>
        {(searchTerm || activeTab !== "ALL") && (
          <button
            onClick={() => {
              setSearchTerm("");
              setActiveTab("ALL");
            }}
            className="text-shop_light_green hover:underline flex items-center gap-1 font-medium"
          >
            <RefreshCw className="w-3 h-3" />
            <span>Đặt lại bộ lọc</span>
          </button>
        )}
      </div>

      {/* Grid or Empty */}
      {filteredVouchers.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredVouchers.map((voucher) => (
            <CustomerVoucherCard key={voucher.code} voucher={voucher} />
          ))}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center py-16 px-4 text-center rounded-2xl bg-white border border-slate-200/80 shadow-xs">
          <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-shop_light_green flex items-center justify-center mb-3">
            <Ticket className="w-7 h-7" />
          </div>
          <h3 className="text-base font-bold text-slate-800">Không tìm thấy mã giảm giá phù hợp</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm">
            Thử thay đổi từ khóa tìm kiếm hoặc chọn danh mục ưu đãi khác để khám phá thêm voucher.
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setSearchTerm("");
              setActiveTab("ALL");
            }}
            className="mt-4 text-xs font-bold rounded-xl"
          >
            Xem tất cả mã
          </Button>
        </div>
      )}
    </div>
  );
}
