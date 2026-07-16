"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { AttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import {
  createAttributeAction,
  updateAttributeAction,
} from "@/features/(catalog)/attributes/actions";
import AttributeForm from "./AttributeForm";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AlertCircle, Loader2, Check, Sliders } from "lucide-react";

interface AttributeFormDialogProps {
  editingAttr: AttributeResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export default function AttributeFormDialog({
  editingAttr,
  open,
  onOpenChange,
  onSuccess,
}: AttributeFormDialogProps) {
  const router = useRouter();
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

  const resetForm = () => {
    setName("");
    setCode("");
    setIsCodeLocked(true);
    setDataType("STRING");
    setUnit("");
    setTags([]);
    setTagInput("");
    setError(null);
  };

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

  // Populate or reset form states when modal opens/changes
  useEffect(() => {
    if (open) {
      if (editingAttr) {
        setName(editingAttr.name);
        setCode(editingAttr.code);
        setIsCodeLocked(true);
        setDataType(editingAttr.dataType as any);
        setUnit(editingAttr.unit || "");
        setError(null);

        if (editingAttr.dataType === "ENUM" && editingAttr.enumValues) {
          if (Array.isArray(editingAttr.enumValues)) {
            setTags(editingAttr.enumValues);
          } else {
            setTags(Object.keys(editingAttr.enumValues));
          }
        } else {
          setTags([]);
        }
        setTagInput("");
      } else {
        resetForm();
      }
    }
  }, [editingAttr, open]);

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setName(val);
    if (isCodeLocked) {
      setCode(convertToSnakeCase(val));
    }
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

  const handleSave = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();

    if (!name.trim()) {
      setError("Tên thuộc tính không được để trống.");
      return;
    }

    setError(null);

    const enumValuesMap =
      dataType === "ENUM"
        ? tags.reduce((acc, tag) => {
            acc[tag] = true;
            return acc;
          }, {} as Record<string, boolean>)
        : null;

    const payload = {
      name: name.trim(),
      code: code.trim() || convertToSnakeCase(name),
      dataType,
      unit: dataType === "NUMBER" && unit.trim() ? unit.trim() : null,
      enumValues: enumValuesMap,
    };

    startTransition(async () => {
      const result = editingAttr
        ? await updateAttributeAction(editingAttr.id, payload)
        : await createAttributeAction(payload);

      if (result.error) {
        setError(result.error);
      } else {
        onSuccess();
        onOpenChange(false);
        resetForm();
      }
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Sliders className="size-5 text-slate-700" />
            {editingAttr ? "Chỉnh sửa thuộc tính" : "Định nghĩa thuộc tính mới"}
          </DialogTitle>
          <DialogDescription>
            {editingAttr
              ? "Chỉnh sửa thông tin định nghĩa thuộc tính nguồn."
              : "Tạo thuộc tính mới trong kho dữ liệu lõi của hệ thống."}
          </DialogDescription>
        </DialogHeader>

        {error && (
          <div className="flex items-center gap-2 p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-200">
            <AlertCircle className="size-4 shrink-0" />
            <p className="font-semibold">{error}</p>
          </div>
        )}

        <div className="py-2">
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
            onSaveSubmit={handleSave}
          />
        </div>

        <DialogFooter className="gap-2">
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
            onClick={() => handleSave()}
            disabled={isPending}
            className="h-9 bg-shop_dark_green hover:bg-shop_btn_dark_green text-white text-xs font-bold px-4 rounded-lg shadow-sm cursor-pointer"
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
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
