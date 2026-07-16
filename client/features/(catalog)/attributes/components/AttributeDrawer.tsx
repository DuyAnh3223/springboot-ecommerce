"use client";

import { useEffect, useState, useTransition } from "react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { AttributeResponse, CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions/get-attributes.action";
import { getGlobalAttributesAction } from "@/features/(catalog)/attributes/actions/get-global-attributes.action";
import { assignCategoryAttributesAction } from "@/features/(catalog)/attributes/actions/assign-attributes.action";
import { updateCategoryAttributeAction } from "@/features/(catalog)/attributes/actions/update-category-attribute.action";
import { removeCategoryAttributeAction } from "@/features/(catalog)/attributes/actions/remove-category-attribute.action";
import AttributeSearchSelect from "./AttributeSearchSelect";
import CategoryAttributeList from "./CategoryAttributeList";
import QuickCreateAttributeModal from "./QuickCreateAttributeModal";
import { Button } from "@/components/ui/button";
import {
  X,
  Sliders,
  AlertCircle,
  Check,
  Loader2,
} from "lucide-react";

interface AttributeDrawerProps {
  category: CategoryResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

interface SelectedAttributeItem {
  id?: number; // PK of category_attribute table
  attributeId: number; // FK attribute id
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

export default function AttributeDrawer({
  category,
  open,
  onOpenChange,
}: AttributeDrawerProps) {
  const [originalAttributes, setOriginalAttributes] = useState<CategoryAttributeResponse[]>([]);
  const [selectedItems, setSelectedItems] = useState<SelectedAttributeItem[]>([]);
  const [globalAttributes, setGlobalAttributes] = useState<AttributeResponse[]>([]);
  
  const [isLoading, setIsLoading] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  // Modal control
  const [quickCreateOpen, setQuickCreateOpen] = useState(false);

  // Load existing category attributes & global attributes
  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [catResult, globalResult] = await Promise.all([
        getAttributesAction(category.id),
        getGlobalAttributesAction({ size: 100 }),
      ]);

      if (catResult.error) {
        setError(catResult.error);
        return;
      }
      
      const catAttrs = (catResult.data || []) as CategoryAttributeResponse[];
      setOriginalAttributes(catAttrs);
      
      // Map existing category attributes to drawer items
      setSelectedItems(
        catAttrs.map((attr) => ({
          id: attr.id,
          attributeId: attr.attributeId,
          name: attr.name,
          code: attr.code,
          dataType: attr.dataType,
          unit: attr.unit,
          enumValues: attr.enumValues,
          isFilterable: attr.isFilterable,
          isVariantDefining: attr.isVariantDefining,
          isCompatibilityKey: attr.isCompatibilityKey,
          isRequired: attr.isRequired,
          sortOrder: attr.sortOrder,
          isNew: false,
        }))
      );

      if (globalResult.data) {
        setGlobalAttributes(globalResult.data.content || []);
      }
    } catch (err: any) {
      console.error("Load attributes drawer data error:", err);
      setError("Không thể tải danh sách thuộc tính.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      loadData();
    }
  }, [category.id, open]);

  const handleSelectAttribute = (gAttr: AttributeResponse) => {
    const newItem: SelectedAttributeItem = {
      attributeId: gAttr.id,
      name: gAttr.name,
      code: gAttr.code,
      dataType: gAttr.dataType,
      unit: gAttr.unit,
      enumValues: gAttr.enumValues,
      isFilterable: true,
      isVariantDefining: false,
      isCompatibilityKey: false,
      isRequired: false,
      sortOrder: selectedItems.length,
      isNew: true,
    };
    setSelectedItems([...selectedItems, newItem]);
  };

  const handleRemoveItem = (code: string) => {
    setSelectedItems(selectedItems.filter((item) => item.code !== code));
  };

  const handleToggleCheckbox = (
    code: string,
    field: "isFilterable" | "isVariantDefining" | "isCompatibilityKey" | "isRequired"
  ) => {
    setSelectedItems(
      selectedItems.map((item) =>
        item.code === code ? { ...item, [field]: !item[field] } : item
      )
    );
  };

  const handleQuickCreateSuccess = (newAttr: AttributeResponse) => {
    // Add to global list cache
    setGlobalAttributes((prev) => [...prev, newAttr]);
    // Immediately select it
    handleSelectAttribute(newAttr);
  };

  // Perform updates
  const handleSave = () => {
    setError(null);
    startTransition(async () => {
      try {
        // 1. Items to unassign (present originally but removed now)
        const toUnassign = originalAttributes.filter(
          (orig) => !selectedItems.some((sel) => sel.attributeId === orig.attributeId)
        );
        for (const item of toUnassign) {
          const res = await removeCategoryAttributeAction(category.id, item.attributeId);
          if (res.error) {
            setError(res.error);
            return;
          }
        }

        // 2. Items to assign (newly selected from global list)
        const toAssign = selectedItems.filter((sel) => sel.isNew || !sel.id);
        if (toAssign.length > 0) {
          const assignRequests = toAssign.map((item) => ({
            attributeId: item.attributeId,
            isFilterable: item.isFilterable,
            isVariantDefining: item.isVariantDefining,
            isCompatibilityKey: item.isCompatibilityKey,
            isRequired: item.isRequired,
            sortOrder: item.sortOrder,
          }));
          const res = await assignCategoryAttributesAction(category.id, assignRequests);
          if (res.error) {
            setError(res.error);
            return;
          }
        }

        // 3. Items to update (existed originally, still selected, but config changed)
        const toUpdate = selectedItems.filter((sel) => {
          if (sel.isNew || !sel.id) return false;
          const orig = originalAttributes.find((o) => o.id === sel.id);
          if (!orig) return false;
          return (
            orig.isFilterable !== sel.isFilterable ||
            orig.isVariantDefining !== sel.isVariantDefining ||
            orig.isCompatibilityKey !== sel.isCompatibilityKey ||
            orig.isRequired !== sel.isRequired ||
            orig.sortOrder !== sel.sortOrder
          );
        });
        for (const item of toUpdate) {
          const res = await updateCategoryAttributeAction(category.id, item.id!, {
            attributeId: item.attributeId,
            isFilterable: item.isFilterable,
            isVariantDefining: item.isVariantDefining,
            isCompatibilityKey: item.isCompatibilityKey,
            isRequired: item.isRequired,
            sortOrder: item.sortOrder,
          });
          if (res.error) {
            setError(res.error);
            return;
          }
        }

        // Reload data from backend to reset states
        await loadData();
        onOpenChange(false);
      } catch (err: any) {
        console.error("Save category attributes error:", err);
        setError("Có lỗi xảy ra khi lưu cấu hình thuộc tính.");
      }
    });
  };

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
