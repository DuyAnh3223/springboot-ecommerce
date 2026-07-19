import React from "react";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Search, Filter } from "lucide-react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { useProductFilters } from "../hooks/useProductFilters";

interface ProductFiltersProps {
  categories: CategoryResponse[];
  filters: ReturnType<typeof useProductFilters>;
}

export function ProductFilters({ categories, filters }: ProductFiltersProps) {
  const {
    searchTerm,
    setSearchTerm,
    selectedCategory,
    setSelectedCategory,
    selectedStatus,
    setSelectedStatus,
    updateQueryParams,
  } = filters;

  return (
    <Card className="border-none shadow-sm bg-white p-4">
      <div className="flex flex-col md:flex-row gap-4 items-center">
        <div className="relative flex-1 w-full">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <Search className="size-4.5" />
          </span>
          <Input
            type="text"
            placeholder="Tìm kiếm sản phẩm theo tên..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 h-10 border-slate-200"
          />
        </div>
        <div className="flex gap-3 w-full md:w-auto shrink-0">
          <div className="flex items-center gap-2">
            <Filter className="size-4 text-slate-400" />
            <select
              value={selectedCategory}
              onChange={(e) => {
                const val = e.target.value;
                setSelectedCategory(val);
                updateQueryParams({ categoryId: val, page: 1 });
              }}
              className="h-10 border border-slate-200 rounded-md px-3 text-sm text-slate-700 bg-white focus:outline-none"
            >
              <option value="">Tất cả danh mục</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-center gap-2">
            <select
              value={selectedStatus}
              onChange={(e) => {
                const val = e.target.value;
                setSelectedStatus(val);
                updateQueryParams({ status: val, page: 1 });
              }}
              className="h-10 border border-slate-200 rounded-md px-3 text-sm text-slate-700 bg-white focus:outline-none"
            >
              <option value="all">Tất cả trạng thái</option>
              <option value="draft">Bản nháp (Draft)</option>
              <option value="published">Đang bán (Published)</option>
            </select>
          </div>
        </div>
      </div>
    </Card>
  );
}
