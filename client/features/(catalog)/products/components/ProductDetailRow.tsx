import React from "react";
import { TableRow, TableCell, Table, TableHeader, TableBody, TableHead } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Loader2, FileText, Package } from "lucide-react";
import { ProductResponse } from "../product.type";

interface ProductDetailRowProps {
  productId: number;
  loading: boolean;
  detail: ProductResponse | undefined;
}

export function ProductDetailRow({ productId, loading, detail }: ProductDetailRowProps) {
  return (
    <TableRow className="bg-slate-50/30 hover:bg-transparent">
      <TableCell colSpan={10} className="p-4 border-b border-slate-100">
        {loading ? (
          <div className="flex items-center justify-center py-6 text-slate-400 text-xs">
            <Loader2 className="size-4 animate-spin mr-1.5" /> Đang tải chi tiết biến thể...
          </div>
        ) : (
          <div className="border border-slate-100 rounded-lg bg-white p-3 shadow-2xs">
            <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <FileText className="size-3.5 text-shop_dark_green" /> Danh sách biến thể (SKUs)
            </h4>
            <Table>
              <TableHeader className="bg-slate-50/50">
                <TableRow>
                  <TableHead className="text-xs text-slate-500 h-8">Ảnh</TableHead>
                  <TableHead className="text-xs text-slate-500 h-8">Mã SKU</TableHead>
                  <TableHead className="text-xs text-slate-500 h-8">Cấu hình biến thể</TableHead>
                  <TableHead className="text-xs text-slate-500 h-8">Giá bán</TableHead>
                  <TableHead className="text-xs text-slate-500 h-8">Tồn kho</TableHead>
                  <TableHead className="text-xs text-slate-500 h-8">Trạng thái</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {detail?.productSkus?.map((sku) => (
                  <TableRow key={sku.id} className="hover:bg-slate-50/30">
                    <TableCell className="py-1">
                      {sku.imageUrl ? (
                        <img
                          src={sku.imageUrl}
                          alt={sku.sku}
                          className="size-8 object-cover rounded border"
                        />
                      ) : (
                        <div className="size-8 bg-slate-50 flex items-center justify-center rounded border text-slate-300">
                          <Package className="size-4" />
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="font-semibold text-slate-700 py-1">{sku.sku}</TableCell>
                    <TableCell className="py-1">
                      <div className="flex flex-wrap gap-1">
                        {Object.entries(sku.attributes || {}).map(([key, val]) => (
                          <Badge
                            key={key}
                            variant="outline"
                            className="bg-slate-50 text-[10px] py-0 px-1.5 border-slate-200"
                          >
                            {key}: {String(val)}
                          </Badge>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="font-medium text-slate-600 py-1">
                      {sku.price.toLocaleString("vi-VN")}đ
                    </TableCell>
                    <TableCell className="text-slate-600 py-1">{sku.stock}</TableCell>
                    <TableCell className="py-1">
                      {sku.isActive ? (
                        <Badge className="bg-emerald-50 text-emerald-700 border-emerald-150 py-0 text-3xs">Hoạt động</Badge>
                      ) : (
                        <Badge className="bg-slate-50 text-slate-400 border-slate-200 py-0 text-3xs">Không hoạt động</Badge>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </TableCell>
    </TableRow>
  );
}
