"use client";

import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Lock, Unlock, X } from "lucide-react";

interface AttributeFormProps {
  name: string;
  onNameChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  code: string;
  onCodeChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  isCodeLocked: boolean;
  onToggleCodeLock: () => void;
  dataType: "STRING" | "NUMBER" | "BOOLEAN" | "ENUM";
  onDataTypeChange: (type: "STRING" | "NUMBER" | "BOOLEAN" | "ENUM") => void;
  unit: string;
  onUnitChange: (val: string) => void;
  tags: string[];
  tagInput: string;
  onTagInputChange: (val: string) => void;
  onTagAdd: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  onTagRemove: (tag: string) => void;
  onSaveSubmit: (e: React.FormEvent) => void;
}

export default function AttributeForm({
  name,
  onNameChange,
  code,
  onCodeChange,
  isCodeLocked,
  onToggleCodeLock,
  dataType,
  onDataTypeChange,
  unit,
  onUnitChange,
  tags,
  tagInput,
  onTagInputChange,
  onTagAdd,
  onTagRemove,
  onSaveSubmit,
}: AttributeFormProps) {
  return (
    <form onSubmit={onSaveSubmit} className="space-y-4">
      {/* Name */}
      <div className="space-y-1.5">
        <label className="text-xs font-semibold text-slate-650">
          Tên thuộc tính <span className="text-destructive">*</span>
        </label>
        <Input
          placeholder="Ví dụ: Dung lượng RAM"
          value={name}
          onChange={onNameChange}
          className="h-9 border-slate-200 focus-visible:ring-slate-300 text-xs bg-white"
        />
      </div>

      {/* Code */}
      <div className="space-y-1.5">
        <label className="text-xs font-semibold text-slate-650 flex items-center gap-1">
          Mã thuộc tính (Auto)
        </label>
        <div className="relative">
          <Input
            placeholder="dung_luong_ram"
            value={code}
            onChange={onCodeChange}
            disabled={isCodeLocked}
            className={`h-9 pr-9 border-slate-200 focus-visible:ring-slate-300 text-xs font-mono bg-white ${
              isCodeLocked ? "text-slate-400 select-none bg-slate-50" : "text-slate-800"
            }`}
          />
          <button
            type="button"
            onClick={onToggleCodeLock}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 cursor-pointer"
          >
            {isCodeLocked ? <Lock className="size-3.5" /> : <Unlock className="size-3.5" />}
          </button>
        </div>
      </div>

      {/* Data Type */}
      <div className="space-y-1.5">
        <label className="text-xs font-semibold text-slate-650">
          Kiểu dữ liệu <span className="text-destructive">*</span>
        </label>
        <select
          value={dataType}
          onChange={(e) => onDataTypeChange(e.target.value as any)}
          className="w-full h-9 px-3 rounded-md border border-slate-200 bg-white text-xs font-medium text-slate-700 cursor-pointer focus:outline-none focus:ring-1 focus:ring-slate-350"
        >
          <option value="STRING">STRING</option>
          <option value="NUMBER">NUMBER</option>
          <option value="BOOLEAN">BOOLEAN</option>
          <option value="ENUM">ENUM</option>
        </select>
      </div>

      {/* Conditional Unit (if NUMBER) */}
      {dataType === "NUMBER" && (
        <div className="space-y-1.5 animate-in slide-in-from-top-1 duration-200">
          <label className="text-xs font-semibold text-slate-650">
            Đơn vị đo (Unit)
          </label>
          <Input
            placeholder="e.g. GB, mAh, kg"
            value={unit}
            onChange={(e) => onUnitChange(e.target.value)}
            className="h-9 border-slate-200 focus-visible:ring-slate-300 text-xs bg-white"
          />
        </div>
      )}

      {/* Conditional Enum values (if ENUM, NUMBER or STRING) */}
      {(dataType === "ENUM" || dataType === "NUMBER" || dataType === "STRING") && (
        <div className="space-y-1.5 animate-in slide-in-from-top-1 duration-200">
          <label className="text-xs font-semibold text-slate-650">
            Giá trị danh sách tùy chọn (Gõ Enter hoặc phẩy để thêm nếu cần giới hạn lựa chọn)
          </label>
          <div className="space-y-2">
            <Input
              placeholder={
                dataType === "NUMBER"
                  ? "Ví dụ: 2, 4, 6, 8 (Gõ số và nhấn Enter)"
                  : dataType === "STRING"
                  ? "Ví dụ: Windows 10, Windows 11 (Gõ và nhấn Enter)"
                  : "Ví dụ: Đỏ, Xanh, Vàng (Gõ và nhấn Enter)"
              }
              value={tagInput}
              onChange={(e) => onTagInputChange(e.target.value)}
              onKeyDown={onTagAdd}
              className="h-9 border-slate-200 focus-visible:ring-slate-300 text-xs bg-white"
            />
            {tags.length > 0 && (
              <div className="flex flex-wrap gap-1.5 p-2 bg-slate-100 rounded-xl border border-slate-200 max-h-[100px] overflow-y-auto">
                {tags.map((tag) => (
                  <Badge
                    key={tag}
                    variant="secondary"
                    className="flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 bg-white border border-slate-200 shadow-xs"
                  >
                    {tag}
                    <button
                      type="button"
                      onClick={() => onTagRemove(tag)}
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
    </form>
  );
}
