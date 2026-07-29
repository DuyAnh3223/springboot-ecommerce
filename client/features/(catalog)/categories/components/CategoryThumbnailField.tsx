"use client";

import { ChangeEvent } from "react";
import { UseFormRegister, FieldErrors } from "react-hook-form";
import { CategoryFormValues } from "../schemas/category.schema";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { UploadCloud, X, Link as LinkIcon } from "lucide-react";

interface CategoryThumbnailFieldProps {
  uploadMode: "file" | "url";
  previewUrl: string;
  uploadError: string | null;
  errors: FieldErrors<CategoryFormValues>;
  isSubmitting: boolean;
  register: UseFormRegister<CategoryFormValues>;
  onModeChange: (mode: "file" | "url") => void;
  onFileChange: (e: ChangeEvent<HTMLInputElement>) => void;
  onRemoveImage: () => void;
}

export function CategoryThumbnailField({
  uploadMode,
  previewUrl,
  uploadError,
  errors,
  isSubmitting,
  register,
  onModeChange,
  onFileChange,
  onRemoveImage,
}: CategoryThumbnailFieldProps) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label>Ảnh thumbnail danh mục</Label>
        <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-md text-xs">
          <button
            type="button"
            className={`px-2 py-0.5 rounded-sm transition-colors cursor-pointer ${
              uploadMode === "file"
                ? "bg-white font-medium shadow-xs text-slate-900"
                : "text-slate-500 hover:text-slate-900"
            }`}
            onClick={() => onModeChange("file")}
          >
            Tải ảnh từ máy
          </button>
          <button
            type="button"
            className={`px-2 py-0.5 rounded-sm transition-colors cursor-pointer ${
              uploadMode === "url"
                ? "bg-white font-medium shadow-xs text-slate-900"
                : "text-slate-500 hover:text-slate-900"
            }`}
            onClick={() => onModeChange("url")}
          >
            Nhập URL
          </button>
        </div>
      </div>

      {previewUrl && (
        <div className="relative group w-full h-32 rounded-lg overflow-hidden border border-slate-200 bg-slate-50 flex items-center justify-center">
          <img
            src={previewUrl}
            alt="Thumbnail preview"
            className="max-h-full object-contain"
          />
          <button
            type="button"
            onClick={onRemoveImage}
            className="absolute top-2 right-2 bg-slate-900/80 hover:bg-slate-900 text-white p-1 rounded-full shadow-md transition-all opacity-90 hover:opacity-100 cursor-pointer"
            title="Gỡ ảnh"
          >
            <X className="size-4" />
          </button>
        </div>
      )}

      {uploadMode === "file" ? (
        <div className="space-y-1.5">
          <label className="flex flex-col items-center justify-center w-full h-24 border-2 border-dashed border-slate-200 hover:border-slate-400 rounded-lg cursor-pointer bg-slate-50/50 hover:bg-slate-50 transition-colors">
            <div className="flex flex-col items-center gap-1 text-slate-500">
              <UploadCloud className="size-6 text-slate-400" />
              <span className="text-xs font-semibold">
                Nhấp để chọn ảnh thumbnail (Tối đa 5MB)
              </span>
              <span className="text-[10px] text-slate-400">
                PNG, JPG, WEBP hoặc GIF
              </span>
            </div>
            <input
              type="file"
              accept="image/png, image/jpeg, image/webp, image/gif"
              className="hidden"
              onChange={onFileChange}
              disabled={isSubmitting}
            />
          </label>
        </div>
      ) : (
        <div className="space-y-1.5">
          <div className="relative">
            <LinkIcon className="absolute left-3 top-2.5 size-4 text-slate-400" />
            <Input
              id="cat-thumbnail"
              placeholder="https://example.com/image.jpg"
              className="pl-9 border-slate-200 focus-visible:ring-slate-400/20"
              {...register("thumbnail")}
            />
          </div>
        </div>
      )}

      {uploadError && (
        <p className="text-xs text-destructive font-medium">{uploadError}</p>
      )}
      {errors.thumbnail?.message && (
        <p className="text-xs text-destructive">{errors.thumbnail.message}</p>
      )}
    </div>
  );
}
