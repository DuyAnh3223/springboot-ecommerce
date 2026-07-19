"use client";

import React from "react";
import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { CheckCircle, Package } from "lucide-react";

interface StepReviewPublishProps {
  productName: string;
  productSlug: string;
  selectedCategoryName: string;
  productThumbnail: string;
  productDescription: string;
  categoryAttributes: CategoryAttributeResponse[];
  nonVariantValues: Record<string, any>;
  skuFields: any[];
}

export function StepReviewPublish({
  productName,
  productSlug,
  selectedCategoryName,
  productThumbnail,
  productDescription,
  categoryAttributes,
  nonVariantValues,
  skuFields,
}: StepReviewPublishProps) {
  const renderMarkdownPreview = (text: string) => {
    if (!text) return <p className="text-slate-400 italic text-sm">Chưa nhập mô tả...</p>;
    let html = text
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      // Headers
      .replace(/^### (.*$)/gim, '<h3 class="text-sm font-bold text-slate-800 mt-2 mb-1">$1</h3>')
      .replace(/^## (.*$)/gim, '<h2 class="text-base font-bold text-slate-800 mt-3 mb-1.5">$1</h2>')
      .replace(/^# (.*$)/gim, '<h1 class="text-lg font-bold text-slate-900 mt-4 mb-2">$1</h1>')
      // Bold
      .replace(/\*\*(.*?)\*\*/g, '<strong class="font-bold text-slate-900">$1</strong>')
      // Italic
      .replace(/\*(.*?)\*/g, '<em class="italic">$1</em>')
      // Lists
      .replace(/^\s*-\s+(.*$)/gim, '<li class="list-disc ml-5 text-slate-700">$1</li>')
      // Code blocks
      .replace(/`(.*?)`/g, '<code class="bg-slate-100 text-rose-600 px-1 py-0.5 rounded font-mono text-xs border">$1</code>')
      // Paragraph breaks
      .replace(/\n/g, "<br />");
    return <div dangerouslySetInnerHTML={{ __html: html }} className="prose prose-sm max-w-none text-slate-750" />;
  };

  return (
    <Card className="border-none shadow-sm bg-white overflow-hidden">
      <CardHeader className="bg-slate-50/50 border-b border-slate-100">
        <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
          <CheckCircle className="size-4.5 text-shop_dark_green" /> Bước 4: Review & Publish (Xem trước và Xuất bản)
        </CardTitle>
        <CardDescription className="text-xs text-slate-450">
          Tổng hợp lại toàn bộ dữ liệu sản phẩm đã thiết lập để kiểm tra lần cuối trước khi phát hành bán công khai.
        </CardDescription>
      </CardHeader>
      <CardContent className="p-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left side: General Info & Attributes details */}
          <div className="lg:col-span-7 space-y-6">
            <div className="space-y-4">
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider pb-2 border-b">
                Thông tin chung sản phẩm
              </h3>
              <div className="grid grid-cols-3 gap-3 text-xs leading-relaxed">
                <div className="text-slate-400 font-medium">Tên sản phẩm:</div>
                <div className="col-span-2 text-slate-800 font-bold">{productName || "—"}</div>

                <div className="text-slate-400 font-medium">Slug sản phẩm:</div>
                <div className="col-span-2 text-slate-800 font-mono">{productSlug || "—"}</div>

                <div className="text-slate-400 font-medium">Danh mục:</div>
                <div className="col-span-2 text-slate-800 font-semibold">{selectedCategoryName}</div>

                <div className="text-slate-400 font-medium">Ảnh đại diện:</div>
                <div className="col-span-2">
                  {productThumbnail ? (
                    <img
                      src={productThumbnail}
                      alt="Cover Thumbnail"
                      className="h-14 object-contain rounded border bg-white p-1"
                    />
                  ) : (
                    <span className="text-slate-400">—</span>
                  )}
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider pb-2 border-b">
                Thông tin thuộc tính tĩnh (EAV)
              </h3>
              {categoryAttributes.filter((ca) => !ca.isVariantDefining).length === 0 ? (
                <p className="text-[10px] text-slate-400 italic">Không có thuộc tính chung.</p>
              ) : (
                <div className="grid grid-cols-2 gap-4 text-xs">
                  {categoryAttributes
                    .filter((ca) => !ca.isVariantDefining)
                    .map((ca) => {
                      const raw = nonVariantValues[ca.code];
                      let displayVal = "—";
                      if (Array.isArray(raw)) {
                        displayVal = raw.join(", ");
                      } else if (raw === true) {
                        displayVal = "Có / Đúng";
                      } else if (raw === false) {
                        displayVal = "Không / Sai";
                      } else if (raw) {
                        displayVal = String(raw);
                      }
                      return (
                        <div key={ca.id} className="p-2.5 bg-slate-50 border rounded-lg">
                          <span className="text-[10px] text-slate-400 font-bold block">{ca.name}:</span>
                          <span className="text-slate-750 font-semibold mt-0.5 block">{displayVal}</span>
                        </div>
                      );
                    })}
                </div>
              )}
            </div>

            <div className="space-y-3">
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider pb-2 border-b">
                Mô tả sản phẩm (Preview)
              </h3>
              <div className="border border-slate-100 rounded-lg p-4 bg-slate-50/50 max-h-[180px] overflow-y-auto">
                {renderMarkdownPreview(productDescription || "")}
              </div>
            </div>
          </div>

          {/* Right side: SKU listings with image micro-thumbnails */}
          <div className="lg:col-span-5 space-y-4">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider pb-2 border-b">
              Danh sách biến thể SKU ({skuFields.length})
            </h3>
            {skuFields.length === 0 ? (
              <p className="text-[10px] text-slate-400 italic text-center py-6">Chưa cấu hình biến thể.</p>
            ) : (
              <div className="space-y-2.5 max-h-[480px] overflow-y-auto pr-1">
                {skuFields.map((s) => (
                  <div key={s.id} className="flex gap-3 p-3 bg-white border rounded-xl shadow-3xs items-center">
                    <div className="shrink-0 size-10 rounded-md border flex items-center justify-center bg-slate-50">
                      {s.imageUrl ? (
                        <img src={s.imageUrl} alt={s.sku} className="size-full object-cover rounded-md" />
                      ) : (
                        <Package className="size-4.5 text-slate-350" />
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <span className="text-xs font-bold text-slate-800 font-mono block truncate">{s.sku}</span>
                      <div className="flex flex-wrap gap-1 mt-1">
                        {Object.entries(s.attributes).map(([k, v]) => (
                          <Badge key={k} variant="outline" className="bg-slate-50 text-[9px] border-slate-200 text-slate-500 py-0 px-1">
                            {k}: {String(v)}
                          </Badge>
                        ))}
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <span className="text-xs font-extrabold text-slate-900 block">{(s.price).toLocaleString("vi-VN")}đ</span>
                      <span className="text-[10px] text-slate-455 font-medium block mt-0.5">Tồn: {s.stock}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
