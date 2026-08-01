"use client";

import { X, RotateCcw } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CatalogFilterParams, CategoryFacetData } from "../types/catalog.types";

interface CatalogFilterChipsProps {
  filters: CatalogFilterParams;
  facets?: CategoryFacetData;
  onUpdateFilters: (newParams: Partial<CatalogFilterParams>) => void;
  onClearAll: () => void;
}

export function CatalogFilterChips({
  filters,
  facets,
  onUpdateFilters,
  onClearAll,
}: CatalogFilterChipsProps) {
  const activeChips: Array<{ id: string; label: string; onRemove: () => void }> = [];

  // Brand chip
  if (filters.brandId && facets?.brands) {
    const brand = facets.brands.find((b) => b.id === filters.brandId);
    if (brand) {
      activeChips.push({
        id: "brand",
        label: `Thương hiệu: ${brand.name}`,
        onRemove: () => onUpdateFilters({ brandId: undefined }),
      });
    }
  }

  // Price Range chip
  if (filters.minPrice !== undefined || filters.maxPrice !== undefined) {
    const minStr = filters.minPrice ? `${filters.minPrice.toLocaleString("vi-VN")}đ` : "0đ";
    const maxStr = filters.maxPrice ? `${filters.maxPrice.toLocaleString("vi-VN")}đ` : "Vô cực";
    activeChips.push({
      id: "price",
      label: `Giá: ${minStr} - ${maxStr}`,
      onRemove: () => onUpdateFilters({ minPrice: undefined, maxPrice: undefined }),
    });
  }

  // In Stock chip
  if (filters.inStock) {
    activeChips.push({
      id: "inStock",
      label: "Còn hàng",
      onRemove: () => onUpdateFilters({ inStock: undefined }),
    });
  }

  // Dynamic Attribute chips
  if (filters.attributes && facets?.attributes) {
    Object.entries(filters.attributes).forEach(([code, values]) => {
      const attrFacet = facets.attributes.find((a) => a.code === code);
      if (attrFacet && values.length > 0) {
        values.forEach((val) => {
          activeChips.push({
            id: `attr_${code}_${val}`,
            label: `${attrFacet.name}: ${val}`,
            onRemove: () => {
              const updated = { ...filters.attributes };
              const nextVals = (updated[code] || []).filter((v) => v !== val);
              if (nextVals.length === 0) {
                delete updated[code];
              } else {
                updated[code] = nextVals;
              }
              onUpdateFilters({ attributes: updated });
            },
          });
        });
      }
    });
  }

  if (activeChips.length === 0) return null;

  return (
    <div className="flex flex-wrap items-center gap-2 mb-4 p-3 bg-white border border-slate-200 rounded-lg shadow-xs">
      <span className="text-xs text-slate-600 font-medium mr-1">Bộ lọc đang chọn:</span>
      {activeChips.map((chip) => (
        <Badge
          key={chip.id}
          variant="secondary"
          className="bg-emerald-50 border border-emerald-200 text-shop_light_green hover:bg-emerald-100 px-2.5 py-1 text-xs flex items-center gap-1.5 transition-all"
        >
          {chip.label}
          <button
            onClick={chip.onRemove}
            className="text-shop_light_green hover:text-emerald-700 rounded-full focus:outline-none"
          >
            <X className="w-3 h-3" />
          </button>
        </Badge>
      ))}

      <Button
        onClick={onClearAll}
        variant="ghost"
        size="sm"
        className="text-xs text-slate-500 hover:text-rose-600 hover:bg-rose-50 h-7 px-2 ml-auto"
      >
        <RotateCcw className="w-3 h-3 mr-1" />
        Xóa tất cả
      </Button>
    </div>
  );
}
