"use client";

import { useState } from "react";
import { createAttributeAction } from "@/features/(catalog)/attributes/actions";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { AlertCircle, Loader2, Plus, Sliders, X } from "lucide-react";

interface QuickCreateAttributeModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: (newAttr: any) => void;
}

export default function QuickCreateAttributeModal({
  open,
  onOpenChange,
  onSuccess,
}: QuickCreateAttributeModalProps) {
  const [name, setName] = useState("");
  const [dataType, setDataType] = useState<"STRING" | "NUMBER" | "BOOLEAN" | "ENUM">("STRING");
  const [unit, setUnit] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const resetForm = () => {
    setName("");
    setDataType("STRING");
    setUnit("");
    setTags([]);
    setTagInput("");
    setError(null);
  };

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

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError("Tên thuộc tính không được để trống.");
      return;
    }
    setError(null);
    setIsLoading(true);

    const enumValuesMap =
      dataType === "ENUM"
        ? tags.reduce((acc, tag) => {
            acc[tag] = true;
            return acc;
          }, {} as Record<string, boolean>)
        : null;

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

    const payload = {
      name: name.trim(),
      code: convertToSnakeCase(name),
      dataType,
      unit: dataType === "NUMBER" && unit.trim() ? unit.trim() : null,
      enumValues: enumValuesMap,
    };

    try {
      const result = await createAttributeAction(payload);
      if (result.error) {
        setError(result.error);
      } else if (result.attribute) {
        onSuccess(result.attribute);
        resetForm();
        onOpenChange(false);
      }
    } catch (err: any) {
      setError("Có lỗi xảy ra khi tạo nhanh thuộc tính.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(val) => {
      if (!val) resetForm();
      onOpenChange(val);
    }}>
      <DialogContent className="sm:max-w-[420px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-1.5">
            <Sliders className="size-4.5 text-shop_light_green" /> Tạo nhanh thuộc tính
          </DialogTitle>
          <DialogDescription>
            Định nghĩa một thuộc tính lõi mới trực tiếp vào kho Master Data.
          </DialogDescription>
        </DialogHeader>

        {error && (
          <div className="flex items-center gap-2 p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-150">
            <AlertCircle className="size-4 shrink-0" />
            <p className="font-semibold">{error}</p>
          </div>
        )}

        <form onSubmit={handleSave} className="space-y-4 py-1">
          {/* Name */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700">
              Tên thuộc tính <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="Ví dụ: GPU"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="h-9 border-slate-200 focus-visible:ring-slate-350"
            />
          </div>

          {/* Data Type */}
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700">
              Kiểu dữ liệu <span className="text-destructive">*</span>
            </label>
            <select
              value={dataType}
              onChange={(e) => {
                setDataType(e.target.value as any);
                setUnit("");
                setTags([]);
              }}
              className="w-full h-9 px-3 rounded-md border border-slate-200 bg-white text-xs font-semibold text-slate-700 focus:outline-none focus:ring-1 focus:ring-slate-350 cursor-pointer"
            >
              <option value="STRING">STRING</option>
              <option value="NUMBER">NUMBER</option>
              <option value="BOOLEAN">BOOLEAN</option>
              <option value="ENUM">ENUM</option>
            </select>
          </div>

          {/* Conditional Unit (if NUMBER) */}
          {dataType === "NUMBER" && (
            <div className="space-y-1 animate-in slide-in-from-top-1 duration-150">
              <label className="text-xs font-bold text-slate-700">
                Đơn vị đo (Unit)
              </label>
              <Input
                placeholder="e.g. Hz, W, kg"
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                className="h-9 border-slate-200 focus-visible:ring-slate-350"
              />
            </div>
          )}

          {/* Conditional Enum values (if ENUM) */}
          {dataType === "ENUM" && (
            <div className="space-y-1 animate-in slide-in-from-top-1 duration-150">
              <label className="text-xs font-bold text-slate-700">
                Giá trị danh sách (Gõ Enter hoặc phẩy để thêm)
              </label>
              <div className="space-y-2">
                <Input
                  placeholder="e.g. RTX 4060, RTX 4070"
                  value={tagInput}
                  onChange={(e) => setTagInput(e.target.value)}
                  onKeyDown={handleTagAdd}
                  className="h-9 border-slate-200 focus-visible:ring-slate-350"
                />
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-1 p-2 bg-slate-50 rounded-lg border border-slate-150 max-h-[80px] overflow-y-auto">
                    {tags.map((tag) => (
                      <Badge
                        key={tag}
                        variant="secondary"
                        className="flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 bg-white border border-slate-200"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="text-slate-400 hover:text-slate-800 focus:outline-none"
                        >
                          <X className="size-2.5" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          <DialogFooter className="pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              className="h-9 text-xs font-semibold px-4 border-slate-200 cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button
              type="submit"
              disabled={isLoading}
              className="h-9 bg-shop_dark_green hover:bg-shop_btn_dark_green text-white text-xs font-bold px-4 rounded-lg shadow-sm cursor-pointer"
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                  Đang tạo...
                </>
              ) : (
                <>
                  <Plus className="mr-1.5 size-3.5" />
                  Tạo thuộc tính
                </>
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
