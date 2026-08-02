"use client";

import React, { useState } from "react";
import { SkuDraft } from "@/features/products/types/sku.draft.type";
import { SkuGalleryDialog, SkuGalleryItem } from "../SkuGalleryDialog";
import { getCanonicalVariantKey } from "@/features/products/utils/reconcile-sku.util";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Images, Zap, Layers, RefreshCw } from "lucide-react";

interface VariantSkuMatrixProps {
  skus: SkuDraft[];
  onChange: (updatedSkus: SkuDraft[]) => void;
  productSlug: string;
  onRegenerateCodes: () => void;
}

export function VariantSkuMatrix({
  skus,
  onChange,
  productSlug,
  onRegenerateCodes,
}: VariantSkuMatrixProps) {
  const [activeGalleryIndex, setActiveGalleryIndex] = useState<number | null>(null);
  const [bulkPrice, setBulkPrice] = useState<string>("");
  const [bulkStock, setBulkStock] = useState<string>("");

  const handleFieldChange = (index: number, field: keyof SkuDraft, value: any) => {
    const updated = [...skus];
    updated[index] = {
      ...updated[index],
      [field]: value,
    };
    onChange(updated);
  };

  const handleApplyBulkPrice = () => {
    const num = Number(bulkPrice);
    if (isNaN(num) || num < 0) return;
    const updated = skus.map((s) => ({ ...s, price: num }));
    onChange(updated);
    setBulkPrice("");
  };

  const handleApplyBulkStock = () => {
    const num = Number(bulkStock);
    if (isNaN(num) || num < 0) return;
    const updated = skus.map((s) => ({ ...s, stock: num }));
    onChange(updated);
    setBulkStock("");
  };

  const activeSku = activeGalleryIndex !== null ? skus[activeGalleryIndex] : null;

  const handleGalleryChange = (newItems: SkuGalleryItem[]) => {
    if (activeGalleryIndex === null) return;
    const updatedImages = newItems.map((item) => ({
      url: item.url || item.previewUrl,
      file: item.file,
      isPrimary: item.isPrimary,
      sortOrder: item.sortOrder,
    }));
    handleFieldChange(activeGalleryIndex, "images", updatedImages);
  };

  return (
    <div className="p-6 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-3 border-b border-slate-100">
        <div>
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <Layers className="w-4 h-4 text-indigo-600" />
            Ma Trận Biến Thể SKU ({skus.length} Tổ Hợp)
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            Quản lý giá, tồn kho và mã SKU riêng cho từng tổ hợp biến thể.
          </p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onRegenerateCodes}
          className="border-slate-200 text-slate-700 hover:bg-slate-50 text-xs font-semibold gap-1.5"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Sinh Lại Mã SKU</span>
        </Button>
      </div>

      {/* Bulk Action Bar */}
      <div className="p-4 rounded-xl bg-slate-50 border border-slate-200/80 flex flex-wrap items-center justify-between gap-4">
        <span className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
          <Zap className="w-4 h-4 text-amber-500" />
          Cập Nhật Hàng Loạt:
        </span>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-1.5">
            <Input
              type="number"
              min={0}
              placeholder="Nhập giá chung..."
              value={bulkPrice}
              onChange={(e) => setBulkPrice(e.target.value)}
              className="h-8 text-xs bg-white w-32 border-slate-200"
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleApplyBulkPrice}
              className="h-8 text-xs font-semibold border-slate-200 hover:bg-white"
            >
              Áp Dụng Giá
            </Button>
          </div>

          <div className="flex items-center gap-1.5">
            <Input
              type="number"
              min={0}
              placeholder="Tồn kho chung..."
              value={bulkStock}
              onChange={(e) => setBulkStock(e.target.value)}
              className="h-8 text-xs bg-white w-32 border-slate-200"
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleApplyBulkStock}
              className="h-8 text-xs font-semibold border-slate-200 hover:bg-white"
            >
              Áp Dụng Tồn
            </Button>
          </div>
        </div>
      </div>

      {/* Table Matrix */}
      <div className="border border-slate-200 rounded-xl overflow-hidden shadow-2xs">
        <Table>
          <TableHeader className="bg-slate-50">
            <TableRow>
              <TableHead className="text-xs font-bold text-slate-700 w-12">#</TableHead>
              <TableHead className="text-xs font-bold text-slate-700">Tổ Hợp Biến Thể</TableHead>
              <TableHead className="text-xs font-bold text-slate-700 w-44">Mã SKU *</TableHead>
              <TableHead className="text-xs font-bold text-slate-700 w-36">Giá (VNĐ) *</TableHead>
              <TableHead className="text-xs font-bold text-slate-700 w-28">Tồn Kho *</TableHead>
              <TableHead className="text-xs font-bold text-slate-700 w-28">Trọng Lượng (g)</TableHead>
              <TableHead className="text-xs font-bold text-slate-700 w-24 text-center">Bộ Ảnh</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {skus.map((sku, idx) => {
              const attrBadges = Object.entries(sku.attributes || {}).map(([k, v]) => `${k}: ${v}`);
              const imgCount = sku.images?.length || 0;
              const rowKey = sku.id ? `sku-${sku.id}` : `combo-${getCanonicalVariantKey(sku.attributes)}`;

              return (
                <TableRow key={rowKey} className="hover:bg-slate-50/50">
                  <TableCell className="text-xs font-mono text-slate-500 font-medium">{idx + 1}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {attrBadges.map((badge, bIdx) => (
                        <Badge
                          key={bIdx}
                          variant="outline"
                          className="text-[11px] font-semibold bg-slate-100 text-slate-800 border-slate-200"
                        >
                          {badge}
                        </Badge>
                      ))}
                    </div>
                  </TableCell>

                  <TableCell>
                    <Input
                      value={sku.sku || ""}
                      onChange={(e) => handleFieldChange(idx, "sku", e.target.value)}
                      className="h-8 text-xs font-mono border-slate-200 focus-visible:ring-rose-500"
                    />
                  </TableCell>

                  <TableCell>
                    <Input
                      type="number"
                      min={0}
                      value={sku.price ?? 0}
                      onChange={(e) => handleFieldChange(idx, "price", Number(e.target.value))}
                      className="h-8 text-xs font-medium border-slate-200 focus-visible:ring-rose-500"
                    />
                  </TableCell>

                  <TableCell>
                    <Input
                      type="number"
                      min={0}
                      value={sku.stock ?? 0}
                      onChange={(e) => handleFieldChange(idx, "stock", Number(e.target.value))}
                      className="h-8 text-xs font-medium border-slate-200 focus-visible:ring-rose-500"
                    />
                  </TableCell>

                  <TableCell>
                    <Input
                      type="number"
                      min={0}
                      value={sku.weightGram ?? 0}
                      onChange={(e) => handleFieldChange(idx, "weightGram", Number(e.target.value))}
                      className="h-8 text-xs border-slate-200"
                    />
                  </TableCell>

                  <TableCell className="text-center">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setActiveGalleryIndex(idx)}
                      className="h-8 px-2 text-xs font-semibold text-indigo-600 hover:text-indigo-700 hover:bg-indigo-50 gap-1"
                    >
                      <Images className="w-3.5 h-3.5" />
                      <span>({imgCount})</span>
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>

      {/* Sku Gallery Dialog */}
      {activeSku && (
        <SkuGalleryDialog
          open={activeGalleryIndex !== null}
          onOpenChange={() => setActiveGalleryIndex(null)}
          skuTitle={activeSku.sku || `SKU #${(activeGalleryIndex ?? 0) + 1}`}
          items={(activeSku.images || []).map((img, i) => ({
            url: img.url,
            previewUrl: img.url,
            isPrimary: Boolean(img.isPrimary),
            sortOrder: img.sortOrder ?? i,
          }))}
          onChange={handleGalleryChange}
        />
      )}
    </div>
  );
}
