"use client";

import { Loader2 } from "lucide-react";
import CategoryAttributeCard from "./CategoryAttributeCard";

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

interface CategoryAttributeListProps {
  selectedItems: SelectedAttributeItem[];
  isLoading: boolean;
  onRemove: (code: string) => void;
  onToggleCheckbox: (
    code: string,
    field: "isFilterable" | "isVariantDefining" | "isCompatibilityKey" | "isRequired" | "isMultiValue"
  ) => void;
}

export default function CategoryAttributeList({
  selectedItems,
  isLoading,
  onRemove,
  onToggleCheckbox,
}: CategoryAttributeListProps) {
  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-10 text-slate-400 gap-2">
        <Loader2 className="size-5 animate-spin text-shop_light_green" />
        <span className="text-xs font-medium">Đang tải cấu hình...</span>
      </div>
    );
  }

  if (selectedItems.length === 0) {
    return (
      <div className="text-center py-12 bg-white border border-dashed border-slate-200 rounded-xl">
        <p className="text-xs font-semibold text-slate-400">
          Chưa gán thuộc tính nào cho danh mục này.
        </p>
        <p className="text-[10px] text-slate-400 mt-1">
          Sử dụng ô tìm kiếm ở trên để chọn.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {selectedItems.map((item) => (
        <CategoryAttributeCard
          key={item.code}
          item={item}
          onRemove={onRemove}
          onToggleCheckbox={onToggleCheckbox}
        />
      ))}
    </div>
  );
}
