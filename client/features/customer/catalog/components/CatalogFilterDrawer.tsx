"use client";

import { useState } from "react";
import { SlidersHorizontal, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CatalogSidebar } from "./CatalogSidebar";
import { CatalogFilterParams, CategoryFacetData } from "@/features/customer/catalog/types/catalog.types";

interface CatalogFilterDrawerProps {
  facets?: CategoryFacetData;
  filters: CatalogFilterParams;
  onUpdateFilters: (newParams: Partial<CatalogFilterParams>) => void;
}

export function CatalogFilterDrawer({ facets, filters, onUpdateFilters }: CatalogFilterDrawerProps) {
  const [open, setOpen] = useState(false);

  return (
    <div className="lg:hidden mb-4">
      <Button
        onClick={() => setOpen(true)}
        variant="outline"
        className="w-full bg-white border-slate-200 text-shop_light_green hover:bg-slate-50 flex items-center justify-center gap-2 text-xs py-2.5"
      >
        <SlidersHorizontal className="w-4 h-4" />
        <span>Mở Bộ lọc kỹ thuật</span>
      </Button>

      {open && (
        <div className="fixed inset-0 z-50 flex bg-black/40 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="relative w-full max-w-xs bg-white h-full p-4 overflow-y-auto shadow-2xl border-r border-slate-200">
            <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-100">
              <span className="font-semibold text-sm text-slate-800">Bộ lọc sản phẩm</span>
              <button
                onClick={() => setOpen(false)}
                className="p-1 rounded-md text-slate-400 hover:text-slate-700 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <CatalogSidebar facets={facets} filters={filters} onUpdateFilters={onUpdateFilters} />

            <div className="mt-4 pt-3 border-t border-slate-100">
              <Button
                onClick={() => setOpen(false)}
                className="w-full bg-shop_light_green hover:bg-shop_dark_green text-white text-xs py-2"
              >
                Xem kết quả
              </Button>
            </div>
          </div>
          <div className="flex-1" onClick={() => setOpen(false)} />
        </div>
      )}
    </div>
  );
}
