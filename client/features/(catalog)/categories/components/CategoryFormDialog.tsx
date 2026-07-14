"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { categorySchema, CategoryFormValues } from "../schemas/category.schema";
import { CategoryResponse } from "../category.type";
import { createCategoryAction, updateCategoryAction } from "../actions";
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
  /** null = Create mode, CategoryResponse = Edit mode */
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
  const isEdit = !!category;
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
    defaultValues: { name: "", slug: "", thumbnail: "" },
  });

  // Auto-generate slug from name (only in create mode)
  const nameValue = watch("name");
  useEffect(() => {
    if (!isEdit && nameValue) {
      const slug = nameValue
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/đ/g, "d")
        .replace(/[^a-z0-9\s-]/g, "")
        .trim()
        .replace(/\s+/g, "-");
      setValue("slug", slug, { shouldValidate: false });
    }
  }, [nameValue, isEdit, setValue]);

  // Populate form when editing
  useEffect(() => {
    if (category) {
      reset({
        name: category.name,
        slug: category.slug,
        thumbnail: category.thumbnail ?? "",
      });
    } else {
      reset({ name: "", slug: "", thumbnail: "" });
    }
    setError(null);
  }, [category, open, reset]);

  const onSubmit = async (values: CategoryFormValues) => {
    setError(null);
    const payload = {
      name: values.name,
      slug: values.slug,
      thumbnail: values.thumbnail || undefined,
    };

    const result = isEdit
      ? await updateCategoryAction(category!.id, payload)
      : await createCategoryAction(payload);

    if (result.error) {
      setError(result.error);
      return;
    }

    onSuccess();
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[480px]">
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

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-1">
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

          {/* Thumbnail */}
          <div className="space-y-1.5">
            <Label htmlFor="cat-thumbnail">URL ảnh thumbnail</Label>
            <Input
              id="cat-thumbnail"
              type="url"
              placeholder="https://example.com/image.jpg"
              className="border-slate-200 focus-visible:ring-slate-400/20"
              {...register("thumbnail")}
            />
            {errors.thumbnail?.message && (
              <p className="text-xs text-destructive">
                {errors.thumbnail.message}
              </p>
            )}
          </div>

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
                  Đang lưu...
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
