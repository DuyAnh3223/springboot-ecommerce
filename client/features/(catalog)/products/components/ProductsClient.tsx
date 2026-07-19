"use client";

import React from "react";
import { useRouter } from "next/navigation";
import { Package, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ProductResponse } from "../product.type";
import { PageResponse } from "@/types/page.type";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { useProductFilters } from "../hooks/useProductFilters";
import { ProductFilters } from "./ProductFilters";
import { ProductTable } from "./ProductTable";
import { ProductModals } from "./ProductModals";

interface ProductsClientProps {
  initialData: PageResponse<ProductResponse>;
  categories: CategoryResponse[];
}

export function ProductsClient({ initialData, categories }: ProductsClientProps) {
  const router = useRouter();
  const filters = useProductFilters();

  return (
    <div className="space-y-4">
      {/* Header toolbar */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            <Package className="size-6 text-shop_dark_green" /> Quản lý sản phẩm
          </h1>
          <p className="text-sm text-slate-500">
            Xem, tạo mới và cấu hình các biến thể sản phẩm (SKU).
          </p>
        </div>
        <Button
          onClick={() => {
            router.push("/admin/products/create");
          }}
          className="bg-shop_dark_green hover:bg-shop_dark_green/90 text-white gap-2 cursor-pointer h-10"
        >
          <Plus className="size-4" /> Thêm sản phẩm
        </Button>
      </div>

      {/* Filters */}
      <ProductFilters categories={categories} filters={filters} />

      {/* Product Table */}
      <ProductTable products={initialData.content} isPending={filters.isPending} />

      {/* Dialog Modals */}
      <ProductModals />
    </div>
  );
}
