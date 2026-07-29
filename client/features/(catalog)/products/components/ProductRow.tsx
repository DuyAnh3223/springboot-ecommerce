import React from "react";
import { TableRow, TableCell } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ChevronUp, ChevronDown, Package, Globe, Edit2, Trash2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { ProductResponse } from "../product.type";
import { useProductDialogStore } from "../stores/product-dialog.store";

interface ProductRowProps {
  product: ProductResponse;
  isExpanded: boolean;
  onToggleExpand: () => void;
}

export function ProductRow({ product, isExpanded, onToggleExpand }: ProductRowProps) {
  const router = useRouter();
  const openDialog = useProductDialogStore((state) => state.openDialog);
  const skusCount = product.skuCount || 0;

  const handleEdit = () => {
    router.push(`/admin/products/${product.id}/edit`);
  };

  return (
    <TableRow className="hover:bg-slate-50/50 border-b border-slate-100">
      <TableCell className="pl-4">
        {skusCount > 0 && (
          <button
            onClick={onToggleExpand}
            className="p-1 hover:bg-slate-100 rounded text-slate-500 cursor-pointer"
          >
            {isExpanded ? <ChevronUp className="size-4" /> : <ChevronDown className="size-4" />}
          </button>
        )}
      </TableCell>
      <TableCell>
        {product.thumbnail ? (
          <img
            src={product.thumbnail}
            alt={product.name}
            className="size-10 object-cover rounded-md border border-slate-150"
          />
        ) : (
          <div className="size-10 bg-slate-100 flex items-center justify-center rounded-md border border-slate-150 text-slate-400">
            <Package className="size-5" />
          </div>
        )}
      </TableCell>
      <TableCell className="font-medium text-slate-800">
        <div>
          <div>{product.name}</div>
          <div className="text-xs text-slate-400 mt-0.5">{product.slug}</div>
        </div>
      </TableCell>
      <TableCell>
        {product.category ? (
          <Badge variant="outline" className="bg-slate-50 text-slate-600 border-slate-200">
            {product.category.name}
          </Badge>
        ) : (
          <span className="text-slate-400">—</span>
        )}
      </TableCell>
      <TableCell className="font-medium text-slate-600">{skusCount}</TableCell>
      <TableCell className="font-medium text-slate-600">{product.totalStock ?? 0}</TableCell>
      <TableCell className="font-medium text-slate-650">
        {product.priceMin !== undefined && product.priceMin !== null ? (
          <span>
            {product.priceMin.toLocaleString("vi-VN")}đ
            {product.priceMax && product.priceMax !== product.priceMin && (
              <> - {product.priceMax.toLocaleString("vi-VN")}đ</>
            )}
          </span>
        ) : (
          <span className="text-slate-400">—</span>
        )}
      </TableCell>
      <TableCell>
        {product.isPublished ? (
          <Badge className="bg-emerald-50 text-emerald-700 border-emerald-250 hover:bg-emerald-100">
            Đang bán
          </Badge>
        ) : (
          <Badge className="bg-slate-100 text-slate-600 border-slate-200 hover:bg-slate-200">
            Bản nháp
          </Badge>
        )}
      </TableCell>
      <TableCell className="font-medium text-slate-600">
        {product.activeSkuCount ?? 0} / {skusCount}
      </TableCell>
      <TableCell className="text-right pr-4 space-x-1.5">
        {product.isPublished ? (
          <Button
            variant="outline"
            size="sm"
            onClick={() => openDialog("unpublish", product)}
            className="border-slate-200 text-amber-600 hover:text-amber-700 hover:bg-amber-50 size-8 p-0 cursor-pointer"
            title="Tạm ẩn sản phẩm"
          >
            <Globe className="size-4 text-amber-500" />
          </Button>
        ) : (
          <Button
            variant="outline"
            size="sm"
            onClick={() => openDialog("publish", product)}
            className="border-slate-200 text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 size-8 p-0 cursor-pointer"
            title="Xuất bản sản phẩm"
          >
            <Globe className="size-4 text-emerald-500" />
          </Button>
        )}
        <Button
          variant="outline"
          size="sm"
          onClick={handleEdit}
          className="border-slate-200 text-slate-600 hover:text-slate-850 size-8 p-0 cursor-pointer"
        >
          <Edit2 className="size-3.5" />
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => openDialog("delete", product)}
          className="border-slate-200 text-red-600 hover:text-red-700 hover:bg-red-50 size-8 p-0 cursor-pointer"
        >
          <Trash2 className="size-3.5" />
        </Button>
      </TableCell>
    </TableRow>
  );
}
