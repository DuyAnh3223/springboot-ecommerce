"use client";

import { Trash2 } from "lucide-react";

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
  isMultiValue: boolean;
  sortOrder: number;
  isNew: boolean;
}

interface CategoryAttributeCardProps {
  item: SelectedAttributeItem;
  onRemove: (code: string) => void;
  onToggleCheckbox: (
    code: string,
    field: "isFilterable" | "isVariantDefining" | "isCompatibilityKey" | "isRequired" | "isMultiValue"
  ) => void;
}

export default function CategoryAttributeCard({
  item,
  onRemove,
  onToggleCheckbox,
}: CategoryAttributeCardProps) {
  return (
    <div className="p-3 bg-white border border-slate-150 rounded-xl shadow-xs space-y-2.5 relative group hover:border-slate-350 transition-all duration-200">
      {/* Remove Button */}
      <button
        onClick={() => onRemove(item.code)}
        className="absolute right-3 top-3 text-slate-400 hover:text-rose-600 p-1 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer"
        title="Bỏ thuộc tính khỏi danh mục"
      >
        <Trash2 className="size-4" />
      </button>

      {/* Info Header */}
      <div className="space-y-0.5 pr-8">
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="text-xs font-bold text-slate-800">
            {item.name}
          </span>
          {item.isNew && (
            <span className="text-[8px] bg-shop_light_green/10 text-shop_light_green border border-shop_light_green/25 font-bold px-1 rounded">
              Mới chọn
            </span>
          )}
        </div>

        <div className="flex items-center gap-1.5">
          <span className="text-[10px] text-slate-450 font-semibold font-mono">
            {item.code}
          </span>
          <span className="text-[10px] text-slate-350">•</span>
          <span className="text-[10px] text-slate-450 font-bold">
            {item.dataType}
            {item.unit && ` (${item.unit})`}
          </span>
        </div>

        {item.dataType === "ENUM" && item.enumValues && (
          <p className="text-[10px] text-slate-400 truncate max-w-[90%] font-medium">
            Giá trị:{" "}
            {Array.isArray(item.enumValues)
              ? item.enumValues.join(", ")
              : Object.keys(item.enumValues).join(", ")}
          </p>
        )}
      </div>

      <hr className="border-slate-100" />

      {/* Context Checkboxes */}
      <div className="grid grid-cols-1 gap-1.5 pt-0.5">
        <label className="flex items-center gap-2 text-xs text-slate-655 font-semibold cursor-pointer select-none">
          <input
            type="checkbox"
            checked={item.isFilterable}
            onChange={() => onToggleCheckbox(item.code, "isFilterable")}
            className="rounded border-slate-300 accent-shop_dark_green cursor-pointer size-4"
          />
          Dùng làm bộ lọc tìm kiếm cho danh mục này
        </label>

        <label className="flex items-center gap-2 text-xs text-slate-655 font-semibold cursor-pointer select-none">
          <input
            type="checkbox"
            checked={item.isVariantDefining}
            onChange={() => onToggleCheckbox(item.code, "isVariantDefining")}
            className="rounded border-slate-300 accent-shop_dark_green cursor-pointer size-4"
          />
          Dùng làm biến thể sản phẩm (SKU)
        </label>

        <label className="flex items-center gap-2 text-xs text-slate-655 font-semibold cursor-pointer select-none">
          <input
            type="checkbox"
            checked={item.isCompatibilityKey}
            onChange={() => onToggleCheckbox(item.code, "isCompatibilityKey")}
            className="rounded border-slate-300 accent-shop_dark_green cursor-pointer size-4"
          />
          Dùng kiểm tra tương thích
        </label>

        <label className="flex items-center gap-2 text-xs text-slate-655 font-semibold cursor-pointer select-none">
          <input
            type="checkbox"
            checked={item.isRequired}
            onChange={() => onToggleCheckbox(item.code, "isRequired")}
            className="rounded border-slate-300 accent-shop_dark_green cursor-pointer size-4"
          />
          Thuộc tính bắt buộc nhập
        </label>

        <label className="flex items-center gap-2 text-xs text-slate-655 font-semibold cursor-pointer select-none">
          <input
            type="checkbox"
            checked={item.isMultiValue}
            onChange={() => onToggleCheckbox(item.code, "isMultiValue")}
            className="rounded border-slate-300 accent-shop_dark_green cursor-pointer size-4"
          />
          Cho phép lưu nhiều giá trị đồng thời (Multi-value)
        </label>
      </div>
    </div>
  );
}
