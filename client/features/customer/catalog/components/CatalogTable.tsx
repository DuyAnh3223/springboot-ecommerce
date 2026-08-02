"use client";

import Image from "next/image";
import Link from "next/link";
import { ArrowUpDown, ArrowUp, ArrowDown, Star } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { AddToCartButton } from "./AddToCartButton";
import { CatalogProductItem, CategoryFacetData, AttributeFacet, CatalogFilterParams } from "@/features/customer/catalog/types/catalog.types";
import { getSafeImageUrl } from "@/shared/utils/image";

interface CatalogTableProps {
  products: CatalogProductItem[];
  facets?: CategoryFacetData;
  filters: CatalogFilterParams;
  onUpdateFilters: (newParams: Partial<CatalogFilterParams>) => void;
  loading?: boolean;
}

export function CatalogTable({
  products,
  facets,
  filters,
  onUpdateFilters,
  loading = false,
}: CatalogTableProps) {
  // Only attributes with isSortable = true drive the dynamic table columns & sorting
  const sortableAttrs = (facets?.attributes || [])
    .filter((a) => a.isSortable)
    .sort((a, b) => a.sortOrder - b.sortOrder);

  const handleSort = (code: string) => {
    const isCurrent = filters.sortBy?.toLowerCase() === code.toLowerCase();
    const nextOrder = isCurrent && filters.order === "asc" ? "desc" : "asc";
    onUpdateFilters({ sortBy: code, order: nextOrder });
  };

  const renderSortIcon = (code: string) => {
    const isCurrent = filters.sortBy?.toLowerCase() === code.toLowerCase();
    if (!isCurrent) return <ArrowUpDown className="w-3 h-3 text-slate-400 group-hover:text-slate-600" />;
    return filters.order === "asc" ? (
      <ArrowUp className="w-3 h-3 text-shop_light_green" />
    ) : (
      <ArrowDown className="w-3 h-3 text-shop_light_green" />
    );
  };

  const formatAttributeValue = (product: CatalogProductItem, attr: AttributeFacet) => {
    const val = product.attributes?.[attr.code];
    if (val === undefined || val === null || val === "") return <span className="text-slate-400">-</span>;

    if (Array.isArray(val)) {
      return val.join(", ");
    }
    if (typeof val === "boolean") {
      return val ? "Có" : "Không";
    }
    if (attr.unit) {
      return `${val} ${attr.unit}`;
    }
    return String(val);
  };

  return (
    <div className="w-full overflow-hidden border border-slate-200/80 rounded-xl bg-white shadow-2xs">
      <div className="overflow-x-auto">
        <Table className="w-full text-[11px] text-left">
          <TableHeader className="bg-slate-100/80 border-b border-slate-200 text-[#191B2A] font-semibold">
            <TableRow className="border-slate-200 hover:bg-transparent">
              {/* Product Info Column */}
              <TableHead
                onClick={() => handleSort("name")}
                className="py-2 px-3 text-[#191B2A] cursor-pointer group hover:text-shop_light_green min-w-[280px] w-[320px]"
              >
                <div className="flex items-center gap-1 font-bold">
                  <span>Tên sản phẩm</span>
                  {renderSortIcon("name")}
                </div>
              </TableHead>

              {/* Brand Column */}
              <TableHead className="py-2 px-2 text-[#191B2A] font-semibold min-w-[90px]">Thương hiệu</TableHead>

              {/* Dynamic Columns for Sortable Category Attributes */}
              {sortableAttrs.map((attr) => (
                <TableHead
                  key={attr.code}
                  onClick={() => handleSort(attr.code)}
                  className="py-2 px-2 text-[#191B2A] cursor-pointer group hover:text-shop_light_green font-semibold whitespace-nowrap"
                >
                  <div className="flex items-center gap-1">
                    <span>
                      {attr.name} {attr.unit ? `(${attr.unit})` : ""}
                    </span>
                    {renderSortIcon(attr.code)}
                  </div>
                </TableHead>
              ))}

             
             

              {/* Price */}
              <TableHead
                onClick={() => handleSort("price")}
                className="py-2 px-3 text-[#191B2A] cursor-pointer group hover:text-shop_light_green text-right min-w-[100px]"
              >
                <div className="flex items-center justify-end gap-1">
                  <span>Giá</span>
                  {renderSortIcon("price")}
                </div>
              </TableHead>

              {/* Action Column */}
              <TableHead className="py-2 px-2 text-center w-[75px] min-w-[75px]"></TableHead>
            </TableRow>
          </TableHeader>

          <TableBody className="divide-y divide-slate-100">
            {loading ? (
              <TableRow>
                <TableCell colSpan={6 + sortableAttrs.length} className="text-center py-6 text-slate-500">
                  Đang tải danh sách sản phẩm...
                </TableCell>
              </TableRow>
            ) : products.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6 + sortableAttrs.length} className="text-center py-6 text-slate-500">
                  Không tìm thấy sản phẩm phù hợp với bộ lọc.
                </TableCell>
              </TableRow>
            ) : (
              products.map((product) => (
                <TableRow key={product.id} className="border-slate-100 hover:bg-slate-50/80 transition-colors">
                  {/* Name & Thumb - Exact PCPartPicker td.td__name style (font 14px, 40px image, 320px width) */}
                  <TableCell className="py-1.5 px-3 min-w-[280px] w-[320px]">
                    <div className="flex items-center gap-3">
                      <div className="relative w-10 h-10 rounded bg-white overflow-hidden border border-slate-200/90 flex-shrink-0 shadow-2xs">
                        {getSafeImageUrl(product.primaryImageUrl || product.thumbnail) ? (
                          <Image
                            src={getSafeImageUrl(product.primaryImageUrl || product.thumbnail)!}
                            alt={product.name}
                            fill
                            sizes="40px"
                            className="object-cover"
                          />
                        ) : (
                          <div className="w-full h-full bg-slate-100 flex items-center justify-center text-[9px] text-slate-400">
                            No Img
                          </div>
                        )}
                      </div>
                      <Link
                        href={`/products/${product.slug}`}
                        className="font-bold text-[14px] text-[#191B2A] hover:text-shop_light_green line-clamp-1 transition-colors leading-snug"
                        title={product.name}
                      >
                        {product.name}
                      </Link>
                    </div>
                  </TableCell>

                  {/* Brand */}
                  <TableCell className="py-1.5 px-2 text-slate-700 font-medium">
                    {product.brand?.name || "-"}
                  </TableCell>

                  {/* Dynamic Attribute Values */}
                  {sortableAttrs.map((attr) => (
                    <TableCell key={attr.code} className="py-1.5 px-2 text-slate-700 whitespace-nowrap">
                      {formatAttributeValue(product, attr)}
                    </TableCell>
                  ))}

                 

                  {/* Price */}
                  <TableCell className="py-1.5 px-3 text-right">
                    {product.priceMin ? (
                      <span className="font-bold text-[#191B2A] text-[12px]">
                        {product.priceMin.toLocaleString("vi-VN")}đ
                      </span>
                    ) : (
                      <span className="text-slate-400 text-[10px]">Liên hệ</span>
                    )}
                  </TableCell>

                  {/* Add to Cart Action */}
                  <TableCell className="py-1.5 px-2 text-center">
                    <AddToCartButton product={product} />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
