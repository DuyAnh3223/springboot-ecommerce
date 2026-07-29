"use client";

import { CategoryResponse } from "../category.type";
import { useCategoryFormDialog } from "../hooks/useCategoryFormDialog";
import { CategoryThumbnailField } from "./CategoryThumbnailField";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { AlertCircle, Loader2, LayoutGrid } from "lucide-react";

interface CategoryFormDialogProps {
  category: CategoryResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export function CategoryFormDialog({
  category,
  open,
  onOpenChange,
  onSuccess,
}: CategoryFormDialogProps) {
  const {
    isEdit,
    error,
    uploadError,
    previewUrl,
    uploadMode,
    register,
    errors,
    isSubmitting,
    handleModeChange,
    handleFileChange,
    handleRemoveImage,
    handleSubmit,
  } = useCategoryFormDialog({ category, open, onOpenChange, onSuccess });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <LayoutGrid className="size-5 text-slate-600" />
            {isEdit ? "Chỉnh sửa danh mục" : "Thêm danh mục mới"}
          </DialogTitle>
          <DialogDescription>
            {isEdit
              ? "Cập nhật thông tin danh mục. Nhấn Lưu để hoàn tất."
              : "Điền thông tin để tạo danh mục mới."}
          </DialogDescription>
        </DialogHeader>

        {error && (
          <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-200">
            <AlertCircle className="size-4 shrink-0" />
            <p className="font-medium">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 py-1">
          {/* Name */}
          <div className="space-y-1.5">
            <Label htmlFor="cat-name">
              Tên danh mục <span className="text-destructive">*</span>
            </Label>
            <Input
              id="cat-name"
              placeholder="Điện thoại & Phụ kiện"
              className="border-slate-200 focus-visible:ring-slate-400/20"
              {...register("name")}
            />
            {errors.name?.message && (
              <p className="text-xs text-destructive">{errors.name.message}</p>
            )}
          </div>

          {/* Slug */}
          <div className="space-y-1.5">
            <Label htmlFor="cat-slug">
              Slug <span className="text-destructive">*</span>
            </Label>
            <Input
              id="cat-slug"
              placeholder="dien-thoai-phu-kien"
              className="border-slate-200 focus-visible:ring-slate-400/20 font-mono text-sm"
              {...register("slug")}
            />
            {errors.slug?.message && (
              <p className="text-xs text-destructive">{errors.slug.message}</p>
            )}
          </div>

          {/* Thumbnail Section */}
          <CategoryThumbnailField
            uploadMode={uploadMode}
            previewUrl={previewUrl}
            uploadError={uploadError}
            errors={errors}
            isSubmitting={isSubmitting}
            register={register}
            onModeChange={handleModeChange}
            onFileChange={handleFileChange}
            onRemoveImage={handleRemoveImage}
          />

          <DialogFooter className="pt-2">
            <DialogClose
              render={
                <Button
                  type="button"
                  variant="outline"
                  className="cursor-pointer"
                />
              }
            >
              Hủy
            </DialogClose>
            <Button
              type="submit"
              disabled={isSubmitting}
              className="bg-slate-900 hover:bg-slate-800 text-white cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang tải ảnh và lưu...
                </>
              ) : isEdit ? (
                "Lưu thay đổi"
              ) : (
                "Tạo danh mục"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
