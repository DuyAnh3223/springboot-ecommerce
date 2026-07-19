import { useEffect, useState, useTransition } from "react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { AttributeResponse, CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions/get-attributes.action";
import { getGlobalAttributesAction } from "@/features/(catalog)/attributes/actions/get-global-attributes.action";
import { assignCategoryAttributesAction } from "@/features/(catalog)/attributes/actions/assign-attributes.action";
import { updateCategoryAttributeAction } from "@/features/(catalog)/attributes/actions/update-category-attribute.action";
import { removeCategoryAttributeAction } from "@/features/(catalog)/attributes/actions/remove-category-attribute.action";
import { useAsyncAction } from "@/hooks";

export interface SelectedAttributeItem {
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
  isMultiValue: boolean;
  sortOrder: number;
  isNew: boolean;
}

export function useCategoryAttributes(
  category: CategoryResponse,
  open: boolean,
  onOpenChange: (open: boolean) => void
) {
  const [originalAttributes, setOriginalAttributes] = useState<CategoryAttributeResponse[]>([]);
  const [selectedItems, setSelectedItems] = useState<SelectedAttributeItem[]>([]);
  const [globalAttributes, setGlobalAttributes] = useState<AttributeResponse[]>([]);
  const [isPending, startTransition] = useTransition();

  // Modal control
  const [quickCreateOpen, setQuickCreateOpen] = useState(false);

  // useAsyncAction shared hook
  const { isLoading, error, setError, run } = useAsyncAction();

  // Load existing category attributes & global attributes
  const loadData = () =>
    run(async () => {
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
          isMultiValue: attr.isMultiValue,
          sortOrder: attr.sortOrder,
          isNew: false,
        }))
      );

      if (globalResult.data) {
        setGlobalAttributes(globalResult.data.content || []);
      }
    });

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
      isMultiValue: false,
      sortOrder: selectedItems.length,
      isNew: true,
    };
    setSelectedItems((prev) => [...prev, newItem]);
  };

  const handleRemoveItem = (code: string) => {
    setSelectedItems((prev) => prev.filter((item) => item.code !== code));
  };

  const handleToggleCheckbox = (
    code: string,
    field: "isFilterable" | "isVariantDefining" | "isCompatibilityKey" | "isMultiValue"
  ) => {
    setSelectedItems((prev) =>
      prev.map((item) => {
        if (item.code === code) {
          const nextValue = !item[field];
          const updated = { ...item, [field]: nextValue };
          // Enforce mutual exclusivity
          if (field === "isVariantDefining" && nextValue) {
            updated.isMultiValue = false;
          } else if (field === "isMultiValue" && nextValue) {
            updated.isVariantDefining = false;
          }
          return updated;
        }
        return item;
      })
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
            isMultiValue: item.isMultiValue,
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
            orig.isMultiValue !== sel.isMultiValue ||
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
            isMultiValue: item.isMultiValue,
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

  return {
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
  };
}
