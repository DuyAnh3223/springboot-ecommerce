"use client";

import { useState } from "react";
import { AttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Search, Plus, X, ArrowRight } from "lucide-react";

interface SelectedAttributeItem {
  id?: number;
  attributeId: number;
  name: string;
  code: string;
  dataType: string;
  unit: string | null;
  enumValues: any;
  isFilterable: boolean;
  isVariantDefining: boolean;
  isCompatibilityKey: boolean;
  isRequired: boolean;
  sortOrder: number;
  isNew: boolean;
}

interface AttributeSearchSelectProps {
  globalAttributes: AttributeResponse[];
  selectedItems: SelectedAttributeItem[];
  onSelect: (attr: AttributeResponse) => void;
  onQuickCreateClick: () => void;
}

export default function AttributeSearchSelect({
  globalAttributes,
  selectedItems,
  onSelect,
  onQuickCreateClick,
}: AttributeSearchSelectProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);

  const filteredSuggestions = globalAttributes.filter(
    (gAttr) =>
      (gAttr.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        gAttr.code.toLowerCase().includes(searchQuery.toLowerCase())) &&
      !selectedItems.some((sel) => sel.code === gAttr.code)
  );

  const handleSelect = (gAttr: AttributeResponse) => {
    onSelect(gAttr);
    setSearchQuery("");
    setShowSuggestions(false);
  };

  return (
    <div className="space-y-1.5 relative shrink-0">
      <label className="text-xs font-bold text-slate-600">
        Tìm & chọn thuộc tính cốt lõi
      </label>
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 size-4" />
          <Input
            placeholder="Gõ tìm: RAM, Màu sắc, Dung lượng..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setShowSuggestions(true);
            }}
            onFocus={() => setShowSuggestions(true)}
            className="pl-9 pr-8 h-9 border-slate-200 focus-visible:ring-slate-350 bg-white text-xs font-semibold"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery("")}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <X className="size-3" />
            </button>
          )}
        </div>

        <Button
          type="button"
          variant="outline"
          onClick={onQuickCreateClick}
          className="h-9 border-slate-200 text-xs font-bold px-3 hover:bg-slate-50 gap-1 cursor-pointer shrink-0"
          title="Tạo nhanh thuộc tính mới"
        >
          <Plus className="size-4 text-shop_light_green" />
          Tạo nhanh
        </Button>
      </div>

      {/* Suggestions Dropdown */}
      {showSuggestions && searchQuery && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setShowSuggestions(false)}
          />
          <div className="absolute left-0 right-0 top-[60px] bg-white border border-slate-200 rounded-xl shadow-lg max-h-[220px] overflow-y-auto z-20 animate-in fade-in slide-in-from-top-1 duration-150">
            {filteredSuggestions.length === 0 ? (
              <div className="p-3 text-center text-xs text-slate-400 font-semibold">
                Không tìm thấy thuộc tính nào phù hợp.
              </div>
            ) : (
              filteredSuggestions.map((gAttr) => (
                <div
                  key={gAttr.id}
                  onClick={() => handleSelect(gAttr)}
                  className="flex items-center justify-between px-4 py-2.5 hover:bg-slate-50 cursor-pointer border-b border-slate-100 last:border-0"
                >
                  <div className="space-y-0.5 text-left">
                    <p className="text-xs font-bold text-slate-800">
                      {gAttr.name}
                    </p>
                    <p className="text-[10px] text-slate-450 font-mono">
                      {gAttr.code}
                    </p>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <Badge
                      variant="outline"
                      className="text-[9px] font-bold bg-slate-50 text-slate-500"
                    >
                      {gAttr.dataType}
                    </Badge>
                    <ArrowRight className="size-3.5 text-slate-300" />
                  </div>
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}
