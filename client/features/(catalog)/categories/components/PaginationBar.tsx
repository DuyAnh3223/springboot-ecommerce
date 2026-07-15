import React from 'react';
import { PageResponse } from "@/types/page.type";
import { CategoryResponse } from "../category.type";
import { Button } from "@/components/ui/button";

interface PaginationBarProps {
  initialData: PageResponse<CategoryResponse>;
  currentPage: number;
  currentSize: number;
  isPending: boolean;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

const PaginationBar = ({
  initialData,
  currentPage,
  currentSize,
  isPending,
  onPageChange,
  onSizeChange,
}: PaginationBarProps) => {
  const { totalElements, totalPages } = initialData;

  // Calculate "Showing X-Y of Z results"
  const from = totalElements === 0 ? 0 : (currentPage - 1) * currentSize + 1;
  const to = Math.min(currentPage * currentSize, totalElements);

  // Generate page numbers
  const pages: number[] = [];
  if (totalPages <= 1) {
    pages.push(1);
  } else {
    // Show a window around current page
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
  }

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 py-2 bg-transparent mt-2">
      {/* Left side: showing count */}
      <div className="text-xs text-slate-500 font-medium">
        Showing <span className="font-semibold text-slate-700">{from}-{to}</span> of <span className="font-semibold text-slate-700">{totalElements}</span> results
      </div>

      {/* Right side: rows selector and page navigation */}
      <div className="flex flex-wrap items-center gap-4.5">
        {/* Rows per page */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-slate-500 font-medium">Rows</span>
          <select
            value={currentSize}
            onChange={(e) => onSizeChange(parseInt(e.target.value))}
            disabled={isPending}
            className="h-8 px-2 rounded-lg border border-slate-200 bg-white text-xs font-semibold text-slate-700 cursor-pointer focus:outline-none focus:ring-1 focus:ring-slate-300"
          >
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
        </div>

        {/* Page navigation buttons */}
        <div className="flex items-center gap-1">
          <Button
            variant="outline"
            disabled={currentPage <= 1 || isPending || totalPages <= 1}
            className="h-8 text-xs font-medium cursor-pointer border-slate-200 text-slate-650 hover:bg-slate-50 px-2.5 rounded-lg"
            onClick={() => onPageChange(currentPage - 1)}
          >
            Previous
          </Button>

          {/* If there are pages, show them */}
          {pages.map((p) => {
            const isActive = p === currentPage;
            return (
              <Button
                key={p}
                variant={isActive ? "default" : "outline"}
                disabled={isPending || totalPages <= 1}
                className={`size-8 text-xs font-semibold cursor-pointer rounded-lg ${
                  isActive
                    ? "bg-shop_dark_green hover:bg-shop_btn_dark_green text-white border-transparent"
                    : "border-slate-200 text-slate-600 hover:text-slate-800 hover:bg-slate-50"
                }`}
                onClick={() => onPageChange(p)}
              >
                {p}
              </Button>
            );
          })}

          <Button
            variant="outline"
            disabled={currentPage >= totalPages || isPending || totalPages <= 1}
            className="h-8 text-xs font-medium cursor-pointer border-slate-200 text-slate-655 hover:bg-slate-50 px-2.5 rounded-lg"
            onClick={() => onPageChange(currentPage + 1)}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
};

export default PaginationBar;