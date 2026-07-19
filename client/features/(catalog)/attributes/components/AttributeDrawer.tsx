"use client";

import React from "react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import AttributeSearchSelect from "./AttributeSearchSelect";
import CategoryAttributeList from "./CategoryAttributeList";
import QuickCreateAttributeModal from "./QuickCreateAttributeModal";
import { Button } from "@/components/ui/button";
import { X, Sliders, AlertCircle, Check, Loader2 } from "lucide-react";
import { useCategoryAttributes } from "../hooks/useCategoryAttributes";

interface AttributeDrawerProps {
  category: CategoryResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function AttributeDrawer({
  category,
  open,
  onOpenChange,
}: AttributeDrawerProps) {
  const {
    selectedItems,
    globalAttributes,
    isLoading,
    isPending,
    error,
    setError,
    quickCreateOpen,
    setQuickCreateOpen,
    handleSelectAttribute,
    handleRemoveItem,
    handleToggleCheckbox,
    handleQuickCreateSuccess,
    handleSave,
  } = useCategoryAttributes(category, open, onOpenChange);

  return (
    <div className="w-full h-[calc(100vh-8.5rem)] min-h-[580px] bg-slate-50 border border-slate-150 rounded-2xl flex flex-col overflow-hidden shadow-xs animate-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-4 bg-white border-b border-slate-100 shrink-0">
        <div className="flex items-center gap-2">
          <Sliders className="size-5 text-slate-700" />
          <div>
            <h2 className="text-sm font-bold text-slate-900 leading-tight">
              Cấu hình thuộc tính danh mục
            </h2>
            <span className="text-xs text-shop_light_green font-bold">
              {category.name}
            </span>
          </div>
        </div>
        <button
          onClick={() => onOpenChange(false)}
          className="p-1 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-700 transition-colors"
        >
          <X className="size-5" />
        </button>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4 flex flex-col pb-20 relative">
        {error && (
          <div className="flex items-center gap-2 p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg shrink-0">
            <AlertCircle className="size-4 shrink-0" />
            <p className="font-semibold">{error}</p>
          </div>
        )}

        {/* Search & Select input component */}
        <AttributeSearchSelect
          globalAttributes={globalAttributes}
          selectedItems={selectedItems}
          onSelect={handleSelectAttribute}
          onQuickCreateClick={() => setQuickCreateOpen(true)}
        />

        {/* Selected Attributes List component */}
        <div className="space-y-2 flex-1 overflow-y-auto pr-1">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
            Thuộc tính của danh mục ({selectedItems.length})
          </h3>

          <CategoryAttributeList
            selectedItems={selectedItems}
            isLoading={isLoading}
            onRemove={handleRemoveItem}
            onToggleCheckbox={handleToggleCheckbox}
          />
        </div>
      </div>

      {/* Sticky Footer */}
      <div className="px-5 py-4 bg-white border-t border-slate-100 flex items-center justify-end gap-3 shrink-0">
        <Button
          type="button"
          variant="outline"
          onClick={() => onOpenChange(false)}
          className="h-9 text-xs font-semibold px-4 cursor-pointer border-slate-200"
        >
          Hủy bỏ
        </Button>
        <Button
          type="submit"
          onClick={handleSave}
          disabled={isPending || isLoading}
          className="h-9 bg-shop_dark_green hover:bg-shop_btn_dark_green text-white text-xs font-bold px-4 cursor-pointer rounded-lg shadow-sm"
        >
          {isPending ? (
            <>
              <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
              Đang lưu...
            </>
          ) : (
            <>
              <Check className="mr-1.5 size-3.5" />
              Lưu cấu hình
            </>
          )}
        </Button>
      </div>

      {/* Quick Create Modal component */}
      <QuickCreateAttributeModal
        open={quickCreateOpen}
        onOpenChange={setQuickCreateOpen}
        onSuccess={handleQuickCreateSuccess}
      />
    </div>
  );
}
