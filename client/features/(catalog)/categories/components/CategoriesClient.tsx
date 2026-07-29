"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { PageResponse } from "@/types/page.type";
import { deleteCategoryAction } from "@/features/(catalog)/categories/actions";
import { CategoryFormDialog } from "@/features/(catalog)/categories/components/CategoryFormDialog";
import Header from "@/features/(catalog)/categories/components/Header";
import AttributeDrawer from "@/features/(catalog)/attributes/components/AttributeDrawer";
import TabsFilter from "@/features/(catalog)/categories/components/TabsFilter";
import Toolbar from "@/features/(catalog)/categories/components/Toolbar";
import DataTable from "@/features/(catalog)/categories/components/DataTable";
import PaginationBar from "@/features/(catalog)/categories/components/PaginationBar";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Trash2 } from "lucide-react";

interface CategoriesClientProps {
  initialData: PageResponse<CategoryResponse>;
}

export function CategoriesClient({ initialData }: CategoriesClientProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();

  // Local search state for debounce
  const [searchTerm, setSearchTerm] = useState(
    searchParams.get("keyword") || ""
  );

  // Dialog state
  const [formOpen, setFormOpen] = useState(false);
  const [editingCategory, setEditingCategory] =
    useState<CategoryResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CategoryResponse | null>(
    null
  );
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [attributeCategory, setAttributeCategory] = useState<CategoryResponse | null>(null);

  // Current query state from URL (default size: 10)
  const currentPage = parseInt(searchParams.get("page") || "1");
  const currentSize = parseInt(searchParams.get("size") || "10");
  const currentIsActive = searchParams.get("isActive");
  const currentSortBy = searchParams.get("sortBy") || "name";
  const currentOrder = searchParams.get("order") || "asc";

  // Debounced keyword search
  useEffect(() => {
    const timer = setTimeout(() => {
      const urlKeyword = searchParams.get("keyword") || "";
      if (searchTerm !== urlKeyword) {
        updateQueryParams({ keyword: searchTerm, page: 1 });
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    setSearchTerm(searchParams.get("keyword") || "");
  }, [searchParams]);

  const updateQueryParams = (
    newParams: Record<string, string | number | boolean | null | undefined>
  ) => {
    const current = new URLSearchParams(Array.from(searchParams.entries()));
    Object.entries(newParams).forEach(([key, value]) => {
      if (value === null || value === undefined || value === "") {
        current.delete(key);
      } else {
        current.set(key, String(value));
      }
    });
    startTransition(() => {
      router.push(`${pathname}?${current.toString()}`);
    });
  };

  const handleSort = (field: string) => {
    const nextOrder =
      currentSortBy === field && currentOrder === "asc" ? "desc" : "asc";
    updateQueryParams({ sortBy: field, order: nextOrder, page: 1 });
  };

  const handleEdit = (category: CategoryResponse) => {
    setEditingCategory(category);
    setFormOpen(true);
  };

  const handleCreate = () => {
    setEditingCategory(null);
    setFormOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    const result = await deleteCategoryAction(deleteTarget.id);
    if (result.error) {
      console.error("Xóa danh mục thất bại:", result.error);
    }
    setIsDeleteOpen(false);
    setDeleteTarget(null);
    router.refresh();
  };

  const isDrawerOpen = !!attributeCategory;

  return (
    <div className="flex gap-4 items-start w-full relative">
      {/* Left side: Category page (60% width if drawer open, 100% otherwise) */}
      <div className={`transition-all duration-300 space-y-2 ${isDrawerOpen ? "w-[60%] shrink-0" : "w-full"}`}>
        {/* Page Header */}
        <Header onAdd={handleCreate} />

        {/* Tabs Filter */}
        <TabsFilter
          currentIsActive={currentIsActive}
          onChange={(val) => updateQueryParams({ isActive: val, page: 1 })}
        />

        {/* Search & Action Toolbar */}
        <Toolbar
          searchTerm={searchTerm}
          onSearchChange={setSearchTerm}
        />

        {/* Main Table Card */}
        <Card className="border border-slate-100 shadow-xs bg-white overflow-hidden">
          <DataTable
            data={initialData.content}
            isPending={isPending}
            currentSortBy={currentSortBy}
            currentOrder={currentOrder}
            onSort={handleSort}
            onEdit={handleEdit}
            onDelete={(cat) => {
              setDeleteTarget(cat);
              setIsDeleteOpen(true);
            }}
            onManageAttributes={(cat) => setAttributeCategory(cat)}
          />
        </Card>

        <PaginationBar
          initialData={initialData}
          currentPage={currentPage}
          currentSize={currentSize}
          isPending={isPending}
          onPageChange={(p) => updateQueryParams({ page: p })}
          onSizeChange={(val) => updateQueryParams({ size: val, page: 1 })}
        />
      </div>

      {/* Right side: Attribute Drawer panel (40% width) */}
      <div className={`transition-all duration-300 sticky top-4 ${isDrawerOpen ? "w-[40%] shrink-0 opacity-100" : "w-0 opacity-0 overflow-hidden"}`}>
        {attributeCategory && (
          <AttributeDrawer
            category={attributeCategory}
            open={isDrawerOpen}
            onOpenChange={(open) => {
              if (!open) setAttributeCategory(null);
            }}
          />
        )}
      </div>

      {/* Create / Edit Dialog */}
      <CategoryFormDialog
        category={editingCategory}
        open={formOpen}
        onOpenChange={setFormOpen}
        onSuccess={() => router.refresh()}
      />

      {/* Delete Confirm Dialog */}
      <Dialog open={isDeleteOpen} onOpenChange={setIsDeleteOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-rose-600 flex items-center gap-2">
              <Trash2 className="size-5" /> Xóa danh mục
            </DialogTitle>
            <DialogDescription className="pt-2">
              Bạn có chắc chắn muốn xóa danh mục{" "}
              <span className="font-bold text-slate-800">
                &quot;{deleteTarget?.name}&quot;
              </span>
              ? Hành động này sẽ ẩn danh mục khỏi hệ thống.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-4">
            <Button
              variant="outline"
              onClick={() => setIsDeleteOpen(false)}
              className="cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button
              onClick={handleDeleteConfirm}
              className="bg-rose-600 hover:bg-rose-700 text-white cursor-pointer"
            >
              Đồng ý xóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
