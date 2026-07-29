"use client";

import React, { useRef, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Upload,
  Trash2,
  Star,
  ArrowUp,
  ArrowDown,
  Images,
  AlertCircle,
} from "lucide-react";

export interface SkuGalleryItem {
  id?: number;
  url?: string; // existing backend S3 key/URL
  file?: File; // newly picked file
  previewUrl: string; // Blob URL for previewing local file or signed S3 URL
  isPrimary: boolean;
  sortOrder: number;
}

interface SkuGalleryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  skuTitle: string;
  items: SkuGalleryItem[];
  onChange: (items: SkuGalleryItem[]) => void;
}

const MAX_IMAGES = 10;
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/gif"];

export function SkuGalleryDialog({
  open,
  onOpenChange,
  skuTitle,
  items,
  onChange,
}: SkuGalleryDialogProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [errorMsg, setErrorMsg] = React.useState<string | null>(null);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    setErrorMsg(null);
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    if (items.length + files.length > MAX_IMAGES) {
      setErrorMsg(`Mỗi SKU chỉ được tối đa ${MAX_IMAGES} ảnh.`);
      return;
    }

    const newItems: SkuGalleryItem[] = [];
    for (const file of files) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        setErrorMsg("Định dạng file không hỗ trợ. Chỉ chấp nhận JPG, PNG, WEBP, GIF.");
        return;
      }
      if (file.size > MAX_FILE_SIZE) {
        setErrorMsg("Dung lượng mỗi file tối đa là 5MB.");
        return;
      }

      const blobUrl = URL.createObjectURL(file);
      newItems.push({
        file,
        previewUrl: blobUrl,
        isPrimary: false,
        sortOrder: items.length + newItems.length,
      });
    }

    let updated = [...items, ...newItems];

    // If gallery had 0 primary, set first item as primary
    if (updated.length > 0 && !updated.some((it) => it.isPrimary)) {
      updated[0] = { ...updated[0], isPrimary: true };
    }

    // Re-index sort order
    updated = updated.map((it, idx) => ({ ...it, sortOrder: idx }));

    onChange(updated);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleRemove = (index: number) => {
    setErrorMsg(null);
    const target = items[index];
    if (target.file && target.previewUrl.startsWith("blob:")) {
      URL.revokeObjectURL(target.previewUrl);
    }

    let updated = items.filter((_, idx) => idx !== index);

    // If removed item was primary and gallery still has items, set first remaining as primary
    if (target.isPrimary && updated.length > 0) {
      updated[0] = { ...updated[0], isPrimary: true };
    }

    updated = updated.map((it, idx) => ({ ...it, sortOrder: idx }));
    onChange(updated);
  };

  const handleSetPrimary = (index: number) => {
    setErrorMsg(null);
    const updated = items.map((it, idx) => ({
      ...it,
      isPrimary: idx === index,
    }));
    onChange(updated);
  };

  const handleMove = (index: number, direction: "up" | "down") => {
    setErrorMsg(null);
    const newIdx = direction === "up" ? index - 1 : index + 1;
    if (newIdx < 0 || newIdx >= items.length) return;

    const updated = [...items];
    const temp = updated[index];
    updated[index] = updated[newIdx];
    updated[newIdx] = temp;

    const reordered = updated.map((it, idx) => ({ ...it, sortOrder: idx }));
    onChange(reordered);
  };

  const handleClearAll = () => {
    setErrorMsg(null);
    items.forEach((it) => {
      if (it.file && it.previewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(it.previewUrl);
      }
    });
    onChange([]);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl bg-white border-none shadow-xl rounded-2xl overflow-hidden p-0">
        <DialogHeader className="px-6 pt-6 pb-4 bg-slate-50 border-b border-slate-100">
          <div className="flex items-center justify-between">
            <DialogTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
              <Images className="size-5 text-shop_dark_green" /> Quản lý Bộ sưu tập ảnh SKU
            </DialogTitle>
            <Badge variant="outline" className="bg-white font-mono text-xs text-slate-600">
              {items.length} / {MAX_IMAGES} ảnh
            </Badge>
          </div>
          <DialogDescription className="text-xs text-slate-500 mt-1">
            SKU: <span className="font-semibold text-slate-700">{skuTitle}</span> — Tải lên tối đa 10 ảnh, chọn 1 ảnh làm đại diện (Primary).
          </DialogDescription>
        </DialogHeader>

        <div className="p-6 space-y-4 max-h-[60vh] overflow-y-auto">
          {errorMsg && (
            <div className="flex items-center gap-2 p-3 text-xs font-medium text-rose-600 bg-rose-50 border border-rose-200 rounded-lg">
              <AlertCircle className="size-4 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Action Bar */}
          <div className="flex items-center justify-between gap-3">
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileSelect}
              multiple
              accept={ALLOWED_TYPES.join(",")}
              className="hidden"
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => fileInputRef.current?.click()}
              disabled={items.length >= MAX_IMAGES}
              className="border-slate-200 text-slate-700 hover:bg-slate-50 text-xs font-semibold gap-1.5 h-9"
            >
              <Upload className="size-4 text-shop_dark_green" /> Tải ảnh từ máy tính
            </Button>

            {items.length > 0 && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleClearAll}
                className="text-rose-600 hover:text-rose-700 hover:bg-rose-50 text-xs font-semibold h-9"
              >
                Xóa tất cả ảnh
              </Button>
            )}
          </div>

          {/* Image List Grid */}
          {items.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-8 border-2 border-dashed border-slate-200 rounded-xl bg-slate-50/50 text-center">
              <Images className="size-10 text-slate-300 mb-2" />
              <p className="text-xs font-semibold text-slate-600">Chưa có ảnh nào cho SKU này</p>
              <p className="text-[11px] text-slate-400 mt-1">Nhấn nút "Tải ảnh từ máy tính" để chọn ảnh</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {items.map((item, idx) => (
                <div
                  key={item.previewUrl || idx}
                  className={`relative group border rounded-xl p-2 bg-white shadow-2xs transition-all ${
                    item.isPrimary ? "ring-2 ring-shop_dark_green border-shop_dark_green" : "border-slate-200"
                  }`}
                >
                  <div className="relative aspect-square rounded-lg overflow-hidden bg-slate-100 border border-slate-100 flex items-center justify-center">
                    <img
                      src={item.previewUrl}
                      alt={`SKU Image ${idx + 1}`}
                      className="w-full h-full object-cover"
                    />

                    {/* Primary Badge */}
                    {item.isPrimary && (
                      <span className="absolute top-1.5 left-1.5 bg-shop_dark_green text-white text-[10px] font-bold px-2 py-0.5 rounded-md shadow-xs flex items-center gap-1">
                        <Star className="size-3 fill-white" /> Ảnh chính
                      </span>
                    )}

                    {/* Ordering / Action Controls Overlay */}
                    <div className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1.5 p-2">
                      {!item.isPrimary && (
                        <Button
                          type="button"
                          size="icon"
                          variant="secondary"
                          onClick={() => handleSetPrimary(idx)}
                          title="Đặt làm ảnh chính"
                          className="size-7 bg-white/90 text-amber-600 hover:bg-white"
                        >
                          <Star className="size-3.5" />
                        </Button>
                      )}

                      <Button
                        type="button"
                        size="icon"
                        variant="secondary"
                        onClick={() => handleMove(idx, "up")}
                        disabled={idx === 0}
                        title="Di chuyển lên trước"
                        className="size-7 bg-white/90 text-slate-700 hover:bg-white disabled:opacity-40"
                      >
                        <ArrowUp className="size-3.5" />
                      </Button>

                      <Button
                        type="button"
                        size="icon"
                        variant="secondary"
                        onClick={() => handleMove(idx, "down")}
                        disabled={idx === items.length - 1}
                        title="Di chuyển xuống sau"
                        className="size-7 bg-white/90 text-slate-700 hover:bg-white disabled:opacity-40"
                      >
                        <ArrowDown className="size-3.5" />
                      </Button>

                      <Button
                        type="button"
                        size="icon"
                        variant="destructive"
                        onClick={() => handleRemove(idx)}
                        title="Xóa ảnh này"
                        className="size-7 bg-rose-600 text-white hover:bg-rose-700"
                      >
                        <Trash2 className="size-3.5" />
                      </Button>
                    </div>
                  </div>

                  <div className="mt-1.5 flex items-center justify-between text-[11px] text-slate-500 px-0.5">
                    <span>Thứ tự: #{idx + 1}</span>
                    {item.file && <span className="text-[10px] text-emerald-600 font-medium">Mới</span>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <DialogFooter className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2">
          <Button
            type="button"
            variant="default"
            onClick={() => onOpenChange(false)}
            className="bg-shop_dark_green hover:bg-shop_dark_green/90 text-white text-xs font-semibold px-5 h-9"
          >
            Hoàn tất
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
