"use client";

import React, { useState } from "react";
import { SkuDraft } from "../../types/sku.draft.type";
import { SkuGalleryDialog, SkuGalleryItem } from "../SkuGalleryDialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Images, Tag, Package, Weight, DollarSign } from "lucide-react";

interface SingleSkuEditorProps {
  skuDraft: SkuDraft;
  onChange: (updatedSku: SkuDraft) => void;
  productSlug: string;
}

export function SingleSkuEditor({ skuDraft, onChange, productSlug }: SingleSkuEditorProps) {
  const [isGalleryOpen, setIsGalleryOpen] = useState(false);
  const [galleryItems, setGalleryItems] = useState<SkuGalleryItem[]>(() => {
    if (!skuDraft.images) return [];
    return skuDraft.images.map((img, idx) => ({
      url: img.url,
      previewUrl: img.url,
      isPrimary: Boolean(img.isPrimary),
      sortOrder: img.sortOrder ?? idx,
    }));
  });

  const handleFieldChange = (field: keyof SkuDraft, value: any) => {
    onChange({
      ...skuDraft,
      [field]: value,
    });
  };

  const handleGalleryChange = (newItems: SkuGalleryItem[]) => {
    setGalleryItems(newItems);
    const updatedImages = newItems.map((item) => ({
      url: item.url || item.previewUrl,
      file: item.file,
      isPrimary: item.isPrimary,
      sortOrder: item.sortOrder,
    }));
    onChange({
      ...skuDraft,
      images: updatedImages,
    });
  };

  return (
    <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-6">
      <div className="flex items-center justify-between pb-3 border-b border-slate-100">
        <div>
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <Package className="w-4 h-4 text-emerald-600" />
            Thông Tin Bán Hàng (Cấu Hình Tiêu Chuẩn)
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Nhập mã SKU, giá bán, tồn kho và bộ sưu tập ảnh cho sản phẩm duy nhất này.
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setIsGalleryOpen(true)}
          className="border-slate-200 text-slate-700 hover:bg-slate-50 font-semibold text-xs gap-1.5"
        >
          <Images className="w-4 h-4 text-indigo-600" />
          <span>Bộ Ảnh SKU ({galleryItems.length})</span>
        </Button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* SKU Code */}
        <div className="space-y-1.5">
          <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1">
            <Tag className="w-3.5 h-3.5 text-slate-400" />
            Mã SKU <span className="text-rose-500">*</span>
          </Label>
          <Input
            value={skuDraft.sku || ""}
            onChange={(e) => handleFieldChange("sku", e.target.value)}
            placeholder={productSlug ? productSlug.toUpperCase() : "SKU-001"}
            className="h-10 text-xs border-slate-200 focus-visible:ring-rose-500"
          />
        </div>

        {/* Price */}
        <div className="space-y-1.5">
          <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1">
            <DollarSign className="w-3.5 h-3.5 text-slate-400" />
            Giá Bán (VNĐ) <span className="text-rose-500">*</span>
          </Label>
          <Input
            type="number"
            min={0}
            value={skuDraft.price ?? 0}
            onChange={(e) => handleFieldChange("price", Number(e.target.value))}
            placeholder="0"
            className="h-10 text-xs border-slate-200 focus-visible:ring-rose-500 font-medium"
          />
        </div>

        {/* Stock */}
        <div className="space-y-1.5">
          <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1">
            <Package className="w-3.5 h-3.5 text-slate-400" />
            Số Lượng Tồn Kho <span className="text-rose-500">*</span>
          </Label>
          <Input
            type="number"
            min={0}
            value={skuDraft.stock ?? 0}
            onChange={(e) => handleFieldChange("stock", Number(e.target.value))}
            placeholder="0"
            className="h-10 text-xs border-slate-200 focus-visible:ring-rose-500 font-medium"
          />
        </div>

        {/* Weight */}
        <div className="space-y-1.5">
          <Label className="text-xs font-semibold text-slate-700 flex items-center gap-1">
            <Weight className="w-3.5 h-3.5 text-slate-400" />
            Trọng Lượng (Gam)
          </Label>
          <Input
            type="number"
            min={0}
            value={skuDraft.weightGram ?? 0}
            onChange={(e) => handleFieldChange("weightGram", Number(e.target.value))}
            placeholder="500"
            className="h-10 text-xs border-slate-200 focus-visible:ring-rose-500"
          />
        </div>
      </div>

      {/* Sku Gallery Dialog */}
      <SkuGalleryDialog
        open={isGalleryOpen}
        onOpenChange={setIsGalleryOpen}
        skuTitle={skuDraft.sku || "Sản Phẩm Đơn"}
        items={galleryItems}
        onChange={handleGalleryChange}
      />
    </div>
  );
}
