"use client";

import { useEffect, useState, use } from "react";
import Link from "next/link";
import { ChevronRight, Home, PackageSearch } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useCatalogFilters } from "@/features/customer/catalog/hooks/useCatalogFilters";
import { catalogService, PageResponse } from "@/features/customer/catalog/services/catalogService";
import { CategoryFacetData, CatalogProductItem } from "@/features/customer/catalog/types/catalog.types";
import { CatalogSidebar } from "@/features/customer/catalog/components/CatalogSidebar";
import { CatalogTable } from "@/features/customer/catalog/components/CatalogTable";
import { CatalogFilterChips } from "@/features/customer/catalog/components/CatalogFilterChips";
import { CatalogFilterDrawer } from "@/features/customer/catalog/components/CatalogFilterDrawer";

interface CategoryPageProps {
  params: Promise<{ slug: string }>;
}

export default function CategoryCatalogPage({ params }: CategoryPageProps) {
  const { slug } = use(params);
  const { filters, updateFilters, clearAllFilters } = useCatalogFilters();

  const [facets, setFacets] = useState<CategoryFacetData | undefined>();
  const [productsPage, setProductsPage] = useState<PageResponse<CatalogProductItem> | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch Category Facets
  useEffect(() => {
    let isMounted = true;
    catalogService
      .getCategoryFacets(slug)
      .then((data) => {
        if (isMounted) setFacets(data);
      })
      .catch((err) => {
        if (isMounted) setError("Không tìm thấy danh mục hoặc danh mục đã bị vô hiệu hóa.");
      });
    return () => {
      isMounted = false;
    };
  }, [slug]);

  // Fetch Catalog Products when filters change
  useEffect(() => {
    let isMounted = true;
    setLoading(true);
    catalogService
      .getCatalogProducts(slug, filters)
      .then((data) => {
        if (isMounted) {
          setProductsPage(data);
          setLoading(false);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError("Lỗi khi tải danh sách sản phẩm.");
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [slug, filters]);

  if (error) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center text-center p-6 bg-slate-50 text-slate-800">
        <PackageSearch className="w-16 h-16 text-slate-400 mb-4" />
        <h1 className="text-xl font-bold text-slate-800 mb-2">{error}</h1>
        <p className="text-sm text-slate-500 mb-6">Vui lòng kiểm tra đường dẫn hoặc quay lại trang chủ.</p>
        <Link href="/">
          <Button variant="outline" className="border-shop_light_green text-shop_light_green hover:bg-emerald-50">
            Quay về trang chủ
          </Button>
        </Link>
      </div>
    );
  }

  const totalElements = productsPage?.totalElements || 0;
  const totalPages = productsPage?.totalPages || 1;
  const currentPage = filters.page || 1;

  return (
    <div className="min-h-screen bg-slate-50/50 text-slate-800 py-3 px-3 md:px-6 max-w-[1800px] mx-auto">
      {/* Breadcrumb Navigation */}
      <nav className="flex items-center gap-1.5 text-[11px] text-slate-500 mb-2">
        <Link href="/" className="hover:text-shop_light_green flex items-center gap-1 transition-colors">
          <Home className="w-3 h-3" />
          <span>Trang chủ</span>
        </Link>
        <ChevronRight className="w-3 h-3 text-slate-400" />
        <span className="text-slate-400">Danh mục</span>
        <ChevronRight className="w-3 h-3 text-slate-400" />
        <span className="text-shop_light_green font-semibold">{facets?.categoryName || slug}</span>
      </nav>

      {/* Title & Stats */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-3 pb-2 border-b border-slate-200">
        <div>
          <h1 className="text-lg md:text-xl font-bold text-slate-900 tracking-tight">
            {facets?.categoryName || "Danh mục sản phẩm"}
          </h1>
          <p className="text-[11px] text-slate-500 mt-0.5">
            Bảng thông số kỹ thuật & so sánh sản phẩm theo chuẩn AB Tech Zone
          </p>
        </div>
        <div className="text-[11px] text-slate-600 bg-white px-2.5 py-1 rounded-md border border-slate-200 shadow-2xs">
          Hiển thị <span className="text-shop_light_green font-semibold">{productsPage?.content.length || 0}</span> /{" "}
          <span className="font-semibold">{totalElements}</span> sản phẩm
        </div>
      </div>

      {/* Mobile Drawer */}
      <CatalogFilterDrawer facets={facets} filters={filters} onUpdateFilters={updateFilters} />

      {/* Active Filter Chips */}
      <CatalogFilterChips
        filters={filters}
        facets={facets}
        onUpdateFilters={updateFilters}
        onClearAll={clearAllFilters}
      />

      {/* Main Catalog Content */}
      <div className="flex flex-col lg:flex-row gap-3.5 items-start">
        {/* Desktop Sidebar */}
        <div className="hidden lg:block">
          <CatalogSidebar facets={facets} filters={filters} onUpdateFilters={updateFilters} />
        </div>

        {/* Catalog Table & Pagination */}
        <div className="flex-1 w-full min-w-0">
          <CatalogTable
            products={productsPage?.content || []}
            facets={facets}
            filters={filters}
            onUpdateFilters={updateFilters}
            loading={loading}
          />

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-3 pt-2 border-t border-slate-200 text-xs">
              <span className="text-slate-500 text-[11px]">
                Trang {currentPage} / {totalPages}
              </span>
              <div className="flex items-center gap-1.5">
                <Button
                  onClick={() => updateFilters({ page: currentPage - 1 })}
                  disabled={currentPage <= 1 || loading}
                  variant="outline"
                  size="sm"
                  className="h-7 text-[11px] bg-white border-slate-200 text-slate-700 hover:bg-slate-50 hover:text-shop_light_green"
                >
                  Trang trước
                </Button>
                <Button
                  onClick={() => updateFilters({ page: currentPage + 1 })}
                  disabled={currentPage >= totalPages || loading}
                  variant="outline"
                  size="sm"
                  className="h-7 text-[11px] bg-white border-slate-200 text-slate-700 hover:bg-slate-50 hover:text-shop_light_green"
                >
                  Trang sau
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
