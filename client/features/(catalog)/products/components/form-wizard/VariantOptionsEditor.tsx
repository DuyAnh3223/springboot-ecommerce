"use client";

import React from "react";
import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { Badge } from "@/components/ui/badge";
import { Layers, Plus, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

interface VariantOptionsEditorProps {
  variantDefs: CategoryAttributeResponse[];
  variantSelections: Record<string, string[]>;
  onChange: (updatedSelections: Record<string, string[]>) => void;
}

export function VariantOptionsEditor({
  variantDefs,
  variantSelections,
  onChange,
}: VariantOptionsEditorProps) {
  const [customInputs, setCustomInputs] = React.useState<Record<string, string>>({});

  const handleToggleOption = (code: string, optionValue: string) => {
    const current = variantSelections[code] || [];
    const exists = current.includes(optionValue);
    const next = exists ? current.filter((v) => v !== optionValue) : [...current, optionValue];

    onChange({
      ...variantSelections,
      [code]: next,
    });
  };

  const handleAddCustomOption = (code: string) => {
    const val = (customInputs[code] || "").trim();
    if (!val) return;

    const current = variantSelections[code] || [];
    if (!current.includes(val)) {
      onChange({
        ...variantSelections,
        [code]: [...current, val],
      });
    }
    setCustomInputs({ ...customInputs, [code]: "" });
  };

  return (
    <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-6">
      <div className="pb-3 border-b border-slate-100">
        <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
          <Layers className="w-4 h-4 text-indigo-600" />
          Tùy Chọn Thuộc Tính Biến Thể
        </h3>
        <p className="text-xs text-slate-500 mt-0.5">
          Chọn các tùy chọn tương ứng cho từng thuộc tính phân loại để hệ thống tự động khởi tạo bảng ma trận SKU.
        </p>
      </div>

      <div className="space-y-5">
        {variantDefs.map((ca) => {
          const selectedValues = variantSelections[ca.code] || [];
          const predefinedOptions = ca.enumValues || [];

          return (
            <div key={ca.code} className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                  {ca.name} <span className="text-slate-400 font-mono text-[11px]">({ca.code})</span>
                </span>
                <Badge variant="outline" className="text-[10px] font-semibold text-indigo-600 bg-indigo-50 border-indigo-200">
                  {selectedValues.length} lựa chọn
                </Badge>
              </div>

              {/* Predefined ENUM values */}
              {predefinedOptions.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {predefinedOptions.map((opt: any) => {
                    const optStr = String(opt);
                    const isSelected = selectedValues.includes(optStr);
                    return (
                      <button
                        key={optStr}
                        type="button"
                        onClick={() => handleToggleOption(ca.code, optStr)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                          isSelected
                            ? "bg-slate-900 text-white border-slate-900 shadow-xs"
                            : "bg-white text-slate-700 border-slate-200 hover:border-slate-400"
                        }`}
                      >
                        {optStr}
                      </button>
                    );
                  })}
                </div>
              )}

              {/* Custom Value Addition */}
              <div className="flex items-center gap-2 pt-1">
                <Input
                  value={customInputs[ca.code] || ""}
                  onChange={(e) => setCustomInputs({ ...customInputs, [ca.code]: e.target.value })}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleAddCustomOption(ca.code);
                    }
                  }}
                  placeholder="Nhập giá trị tùy chỉnh rồi bấm Thêm..."
                  className="h-8 text-xs bg-white border-slate-200 focus-visible:ring-indigo-500 max-w-xs"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => handleAddCustomOption(ca.code)}
                  className="h-8 text-xs font-semibold border-slate-200 hover:bg-slate-100 gap-1"
                >
                  <Plus className="w-3.5 h-3.5" /> Thêm
                </Button>
              </div>

              {/* Selected List */}
              {selectedValues.length > 0 && (
                <div className="flex flex-wrap gap-1 pt-2 border-t border-slate-200/60">
                  {selectedValues.map((val) => (
                    <span
                      key={val}
                      className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md bg-indigo-50 border border-indigo-200 text-indigo-800 text-[11px] font-bold"
                    >
                      {val}
                      <button
                        type="button"
                        onClick={() => handleToggleOption(ca.code, val)}
                        className="hover:text-rose-600 text-indigo-400 ml-0.5"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

