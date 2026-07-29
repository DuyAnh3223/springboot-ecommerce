"use client";

import React from "react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Info } from "lucide-react";
import { CategoryTreeSelector } from "./CategoryTreeSelector";
import { MarkdownEditor } from "./MarkdownEditor";

interface StepBasicInfoProps {
  register: any;
  errors: any;
  isEdit: boolean;
  categories: CategoryResponse[];
  selectedCategoryId: number;
  setValue: any;
  setError: (err: string | null) => void;
  productDescription: string;
  onCategorySelect: (id: number) => void;
}

export function StepBasicInfo({
  register,
  errors,
  isEdit,
  categories,
  selectedCategoryId,
  setValue,
  setError,
  productDescription,
  onCategorySelect,
}: StepBasicInfoProps) {
  return (
    <Card className="border-none shadow-sm bg-white overflow-hidden">
      <CardHeader className="bg-slate-50/50 border-b border-slate-100">
        <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
          <Info className="size-4.5 text-shop_dark_green" /> Bước 1: Thông tin cơ bản & Phân loại
        </CardTitle>
        <CardDescription className="text-xs text-slate-450">
          Nhập các thông tin tổng quan của sản phẩm và chọn danh mục cấp cuối cùng để cấu hình các thuộc tính.
        </CardDescription>
      </CardHeader>
      <CardContent className="p-6 space-y-6">
        {/* Product Name */}
        <div className="space-y-2">
          <Label className="text-slate-750 font-bold text-xs flex items-center gap-1">
            Tên sản phẩm <span className="text-red-500">*</span>
          </Label>
          <Input
            type="text"
            {...register("name")}
            placeholder="Nhập tên sản phẩm (ví dụ: Điện thoại Apple iPhone 15 Pro Max 256GB)"
            className="border-slate-200 h-10 focus-visible:ring-shop_dark_green/10"
          />
          {errors.name && <p className="text-rose-500 text-2xs font-semibold">{errors.name.message}</p>}
        </div>

        {/* Slug & Category Select */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <Label className="text-slate-750 font-bold text-xs flex items-center gap-1">
              Slug sản phẩm <span className="text-red-500">*</span>
            </Label>
            <Input
              type="text"
              {...register("slug")}
              placeholder="slug-san-pham"
              className="border-slate-200 h-10 focus-visible:ring-shop_dark_green/10 font-mono text-xs"
              disabled={isEdit}
            />
            {errors.slug && <p className="text-rose-500 text-2xs font-semibold">{errors.slug.message}</p>}
          </div>

          <div className="space-y-2">
            <Label className="text-slate-750 font-bold text-xs flex items-center gap-1">
              Danh mục sản phẩm (Category) <span className="text-red-500">*</span>
            </Label>
            <CategoryTreeSelector
              categories={categories}
              selectedCategoryId={selectedCategoryId}
              onSelect={onCategorySelect}
              isEdit={isEdit}
              setError={setError}
            />
            {errors.categoryId && (
              <p className="text-rose-500 text-2xs font-semibold">{errors.categoryId.message}</p>
            )}
          </div>
        </div>

        {/* Markdown Description Editor */}
        <MarkdownEditor
          value={productDescription}
          onChange={(val) => setValue("description", val, { shouldValidate: true })}
          registerProps={register("description")}
        />
      </CardContent>
    </Card>
  );
}
