"use client";

import { useEffect, useState, useTransition } from "react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { AttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions/get-attributes.action";
import { createAttributeAction } from "@/features/(catalog)/attributes/actions/create-attribute.action";
import { deleteAttributeAction } from "@/features/(catalog)/attributes/actions/delete-attribute.action";
import AttributeList from "./AttributeList";
import AttributeForm from "./AttributeForm";
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

export default function AttributeDrawer({
  category,
  open,
  onOpenChange,
}: AttributeDrawerProps) {
  const [attributes, setAttributes] = useState<AttributeResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [name, setName] = useState("");
  const [code, setCode] = useState("");
  const [isCodeLocked, setIsCodeLocked] = useState(true);
  const [dataType, setDataType] = useState<"STRING" | "NUMBER" | "BOOLEAN" | "ENUM">("STRING");
  const [unit, setUnit] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");

  const [isFilterable, setIsFilterable] = useState(true);
  const [isVariantDefining, setIsVariantDefining] = useState(false);
  const [isCompatibilityKey, setIsCompatibilityKey] = useState(false);
  const [sortOrder, setSortOrder] = useState(0);

  // Load existing attributes
  const fetchAttributes = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await getAttributesAction(category.id, {
        size: 100, // Load all attributes
        sortBy: "sortOrder",
        order: "asc",
      });
      if (result.error) {
        setError(result.error);
      } else if (result.data) {
        setAttributes(result.data.content || []);
      }
    } catch (err: any) {
      console.error("Fetch attributes error:", err);
      setError("Không thể tải danh sách thuộc tính.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      fetchAttributes();
      resetForm();
    }
  }, [category.id, open]);

  const resetForm = () => {
    setName("");
    setCode("");
    setIsCodeLocked(true);
    setDataType("STRING");
    setUnit("");
    setTags([]);
    setTagInput("");
    setIsFilterable(true);
    setIsVariantDefining(false);
    setIsCompatibilityKey(false);
    setSortOrder(0);
    setError(null);
  };

  // Auto-generate code from name
  const convertToSnakeCase = (str: string) => {
    return str
      .trim()
      .toLowerCase()
      .replace(/đ/g, "d")
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9\s-]/g, "")
      .trim()
      .replace(/[\s\-\.]+/g, "_")
      .replace(/_+/g, "_")
      .replace(/^_+|_+$/g, "");
  };

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setName(val);
    if (isCodeLocked) {
      setCode(convertToSnakeCase(val));
    }
  };

  // Add enum tag
  const handleTagAdd = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      const cleaned = tagInput.trim();
      if (cleaned && !tags.includes(cleaned)) {
        setTags([...tags, cleaned]);
      }
      setTagInput("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((t) => t !== tagToRemove));
  };

  // Delete attribute
  const handleDeleteAttribute = async (attrId: number) => {
    if (!confirm("Bạn có chắc chắn muốn xóa thuộc tính này?")) return;
    setError(null);
    startTransition(async () => {
      const result = await deleteAttributeAction(attrId);
      if (result.error) {
        setError(result.error);
      } else {
        fetchAttributes();
      }
    });
  };

  // Save attribute
  const handleSave = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    
    if (!name.trim()) {
      setError("Tên thuộc tính không được để trống.");
      return;
    }

    setError(null);
    
    // Map list tags to object structure for Enum Map validation
    const enumValuesMap =
      dataType === "ENUM"
        ? tags.reduce((acc, tag) => {
            acc[tag] = true;
            return acc;
          }, {} as Record<string, boolean>)
        : null;

    const payload = {
      categoryId: category.id,
      name: name.trim(),
      code: code.trim() || convertToSnakeCase(name),
      dataType,
      unit: dataType === "NUMBER" && unit.trim() ? unit.trim() : null,
      enumValues: enumValuesMap,
      isFilterable,
      isVariantDefining,
      isCompatibilityKey,
      sortOrder,
    };

    startTransition(async () => {
      const result = await createAttributeAction(payload);
      if (result.error) {
        setError(result.error);
      } else {
        resetForm();
        fetchAttributes();
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
              Quản lý thuộc tính
            </h2>
            <span className="text-xs text-slate-500 font-medium font-mono">
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

      {/* Scrollable Body */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-6 pb-20">
        {error && (
          <div className="flex items-center gap-2 p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-200">
            <AlertCircle className="size-4 shrink-0" />
            <p className="font-semibold">{error}</p>
          </div>
        )}

        {/* Part 1: List of Attributes */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Danh sách thuộc tính
          </h3>
          <AttributeList
            attributes={attributes}
            isLoading={isLoading}
            isPending={isPending}
            onDelete={handleDeleteAttribute}
          />
        </div>

        <hr className="border-slate-100" />

        {/* Part 2: Form to Add New Attribute */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
            Thêm thuộc tính mới
          </h3>
          <AttributeForm
            name={name}
            onNameChange={handleNameChange}
            code={code}
            onCodeChange={(e) => setCode(e.target.value)}
            isCodeLocked={isCodeLocked}
            onToggleCodeLock={() => setIsCodeLocked(!isCodeLocked)}
            dataType={dataType}
            onDataTypeChange={(type) => {
              setDataType(type);
              setUnit("");
              setTags([]);
            }}
            unit={unit}
            onUnitChange={setUnit}
            tags={tags}
            tagInput={tagInput}
            onTagInputChange={setTagInput}
            onTagAdd={handleTagAdd}
            onTagRemove={handleRemoveTag}
            isFilterable={isFilterable}
            onFilterableChange={setIsFilterable}
            isVariantDefining={isVariantDefining}
            onVariantDefiningChange={setIsVariantDefining}
            isCompatibilityKey={isCompatibilityKey}
            onCompatibilityKeyChange={setIsCompatibilityKey}
            sortOrder={sortOrder}
            onSortOrderChange={setSortOrder}
            onSaveSubmit={handleSave}
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
          onClick={() => handleSave()}
          disabled={isPending}
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
              Lưu lại
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
