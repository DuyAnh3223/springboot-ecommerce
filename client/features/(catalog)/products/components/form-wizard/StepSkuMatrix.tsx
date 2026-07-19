"use client";

import React from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Package, Sliders, CheckCircle, Loader2 } from "lucide-react";

interface StepSkuMatrixProps {
  skuFields: any[];
  updateSkuField: (index: number, value: any) => void;
  bulkPrice: string;
  setBulkPrice: (val: string) => void;
  bulkStock: string;
  setBulkStock: (val: string) => void;
  bulkImage: string;
  setBulkImage: (val: string) => void;
  handleBulkApply: () => void;
}

export function StepSkuMatrix({
  skuFields,
  updateSkuField,
  bulkPrice,
  setBulkPrice,
  bulkStock,
  setBulkStock,
  bulkImage,
  setBulkImage,
  handleBulkApply,
}: StepSkuMatrixProps) {
  return (
    <Card className="border-none shadow-sm bg-white overflow-hidden">
      <CardHeader className="bg-slate-50/50 border-b border-slate-100">
        <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
          <Package className="size-4.5 text-shop_dark_green" /> Bước 3: Quản lý SKU & Giá (SKU Cartesian Matrix)
        </CardTitle>
        <CardDescription className="text-xs text-slate-450">
          Thiết lập chi tiết về mã định danh SKU, giá bán, tồn kho và hình ảnh riêng biệt cho từng biến thể sản phẩm.
        </CardDescription>
      </CardHeader>
      <CardContent className="p-6 space-y-6">
        {skuFields.length === 0 ? (
          <div className="text-center py-10 text-slate-455 border border-dashed border-slate-200 rounded-xl bg-slate-50/30">
            <Package className="size-10 text-slate-300 mx-auto mb-2" />
            <p className="text-xs font-semibold">Chưa có dữ liệu biến thể SKU.</p>
            <p className="text-[10px] text-slate-400 mt-1">
              Vui lòng hoàn thành chọn thuộc tính biến thể ở Bước 2 và ấn "Sinh mã bản xem trước SKU".
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Bulk Action Bar */}
            <div className="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-3">
              <h4 className="text-xs font-bold text-slate-750 flex items-center gap-1">
                <Sliders className="size-3.5 text-shop_dark_green" /> [ Áp dụng hàng loạt thông số ]
              </h4>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
                <div className="space-y-1.5">
                  <Label className="text-[10px] font-bold text-slate-600">Giá bán chung (đ)</Label>
                  <Input
                    type="number"
                    placeholder="Ví dụ: 15000000"
                    value={bulkPrice}
                    onChange={(e) => setBulkPrice(e.target.value)}
                    className="h-8.5 border-slate-200 text-xs focus-visible:ring-shop_dark_green/10"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className="text-[10px] font-bold text-slate-600">Tồn kho chung</Label>
                  <Input
                    type="number"
                    placeholder="Ví dụ: 50"
                    value={bulkStock}
                    onChange={(e) => setBulkStock(e.target.value)}
                    className="h-8.5 border-slate-200 text-xs focus-visible:ring-shop_dark_green/10"
                  />
                </div>
                <div className="space-y-1.5">
                  <Label className="text-[10px] font-bold text-slate-600">URL ảnh SKU chung</Label>
                  <Input
                    type="text"
                    placeholder="Http://..."
                    value={bulkImage}
                    onChange={(e) => setBulkImage(e.target.value)}
                    className="h-8.5 border-slate-200 text-xs focus-visible:ring-shop_dark_green/10"
                  />
                </div>
                <Button
                  type="button"
                  onClick={handleBulkApply}
                  className="bg-slate-900 hover:bg-slate-800 text-white cursor-pointer h-8.5 text-xs font-bold"
                >
                  Áp dụng cho tất cả
                </Button>
              </div>
            </div>

            {/* SKU Matrix Table */}
            <div className="border border-slate-100 rounded-xl overflow-hidden bg-white shadow-2xs">
              <Table>
                <TableHeader className="bg-slate-50">
                  <TableRow>
                    <TableHead className="text-slate-600 font-bold text-2xs py-3 w-1/4">Biến thể</TableHead>
                    <TableHead className="text-slate-600 font-bold text-2xs py-3 w-1/4">Mã SKU định danh (*)</TableHead>
                    <TableHead className="text-slate-600 font-bold text-2xs py-3 w-1/6">Giá bán (đ) (*)</TableHead>
                    <TableHead className="text-slate-600 font-bold text-2xs py-3 w-1/6">Tồn kho (*)</TableHead>
                    <TableHead className="text-slate-600 font-bold text-2xs py-3 w-1/4">Ảnh URL SKU</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {skuFields.map((field, index) => (
                    <TableRow key={field.id} className="hover:bg-slate-50/30">
                      <TableCell className="py-2.5">
                        <div className="flex flex-wrap gap-1">
                          {Object.entries(field.attributes || {}).length === 0 ? (
                            <Badge variant="outline" className="bg-slate-50 text-[10px] font-semibold text-slate-500">
                              Mặc định (Default)
                            </Badge>
                          ) : (
                            Object.entries(field.attributes || {}).map(([k, v]) => (
                              <Badge key={k} variant="outline" className="bg-slate-50 text-[10px] border-slate-200 text-slate-700 py-0.5">
                                {k}: {String(v)}
                              </Badge>
                            ))
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="py-2.5">
                        <Input
                          type="text"
                          value={field.sku}
                          onChange={(e) => {
                            updateSkuField(index, { ...field, sku: e.target.value });
                          }}
                          className="h-8 border-slate-200 text-xs max-w-[200px] font-mono focus-visible:ring-shop_dark_green/10"
                        />
                      </TableCell>
                      <TableCell className="py-2.5">
                        <Input
                          type="number"
                          value={field.price}
                          onChange={(e) => {
                            updateSkuField(index, { ...field, price: parseInt(e.target.value) || 0 });
                          }}
                          className="h-8 border-slate-200 text-xs max-w-[120px] focus-visible:ring-shop_dark_green/10"
                        />
                      </TableCell>
                      <TableCell className="py-2.5">
                        <Input
                          type="number"
                          value={field.stock}
                          onChange={(e) => {
                            updateSkuField(index, { ...field, stock: parseInt(e.target.value) || 0 });
                          }}
                          className="h-8 border-slate-200 text-xs max-w-[80px] focus-visible:ring-shop_dark_green/10"
                        />
                      </TableCell>
                      <TableCell className="py-2.5">
                        <div className="flex items-center gap-2">
                          <Input
                            type="text"
                            placeholder="URL ảnh"
                            value={field.imageUrl || ""}
                            onChange={(e) => {
                              updateSkuField(index, { ...field, imageUrl: e.target.value });
                            }}
                            className="h-8 border-slate-200 text-xs focus-visible:ring-shop_dark_green/10 flex-1 min-w-[120px]"
                          />
                          {field.imageUrl && (
                            <img
                              src={field.imageUrl}
                              alt="SKU image preview"
                              className="size-7.5 rounded object-cover border bg-white"
                              onError={(e) => {
                                (e.target as HTMLImageElement).style.display = "none";
                              }}
                            />
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            {/* Submit SKUs action is now automated in the final step */}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
