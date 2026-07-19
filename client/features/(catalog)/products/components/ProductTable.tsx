import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Table, TableBody, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Loader2, Package } from "lucide-react";
import { ProductResponse } from "../product.type";
import { useProductExpansion } from "../hooks/useProductExpansion";
import { ProductRow } from "./ProductRow";
import { ProductDetailRow } from "./ProductDetailRow";

interface ProductTableProps {
  products: ProductResponse[];
  isPending: boolean;
}

export function ProductTable({ products, isPending }: ProductTableProps) {
  const { isExpanded, isLoading, getDetail, toggleExpand } = useProductExpansion();

  return (
    <Card className="border-none shadow-sm bg-white overflow-hidden relative">
      {isPending && (
        <div className="absolute inset-0 bg-white/60 backdrop-blur-xs flex items-center justify-center z-10">
          <div className="flex items-center gap-2 bg-slate-900 text-white px-4 py-2 rounded-full shadow-md">
            <Loader2 className="size-4 animate-spin" />
            <span className="text-xs font-semibold">Đang cập nhật...</span>
          </div>
        </div>
      )}

      {products.length === 0 ? (
        <CardContent className="flex flex-col items-center justify-center py-20 text-center">
          <div className="p-4 bg-slate-50 rounded-full text-slate-400 mb-4 border border-slate-100">
            <Package className="size-12" />
          </div>
          <h3 className="text-lg font-bold text-slate-700">Không tìm thấy sản phẩm</h3>
          <p className="text-sm text-slate-400 mt-1 max-w-sm">
            Hãy thử thay đổi bộ lọc hoặc thêm mới sản phẩm.
          </p>
        </CardContent>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50/50 border-b border-slate-100">
              <TableRow>
                <TableHead className="w-10 pl-4"></TableHead>
                <TableHead className="w-16">Ảnh</TableHead>
                <TableHead className="font-semibold text-slate-600">Tên sản phẩm</TableHead>
                <TableHead className="font-semibold text-slate-600">Danh mục</TableHead>
                <TableHead className="font-semibold text-slate-600">Số SKU</TableHead>
                <TableHead className="font-semibold text-slate-600">Tổng tồn kho</TableHead>
                <TableHead className="font-semibold text-slate-600">Khoảng giá</TableHead>
                <TableHead className="font-semibold text-slate-600">Trạng thái</TableHead>
                <TableHead className="font-semibold text-slate-600">SKU hoạt động</TableHead>
                <TableHead className="font-semibold text-slate-650 text-right pr-4">Hành động</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {products.map((product) => {
                const expanded = isExpanded(product.id);
                return (
                  <React.Fragment key={product.id}>
                    <ProductRow
                      product={product}
                      isExpanded={expanded}
                      onToggleExpand={() => toggleExpand(product.id)}
                    />
                    {expanded && (
                      <ProductDetailRow
                        productId={product.id}
                        loading={isLoading(product.id)}
                        detail={getDetail(product.id)}
                      />
                    )}
                  </React.Fragment>
                );
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </Card>
  );
}
