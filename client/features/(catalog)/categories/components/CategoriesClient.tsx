"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { CategoryResponse, PageResponse } from "../category.type";
import { deleteCategoryAction } from "../actions";
import { CategoryFormDialog } from "./CategoryFormDialog";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Search,
  Plus,
  Edit2,
  Trash2,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  LayoutGrid,
  Loader2,
  Image as ImageIcon,
} from "lucide-react";

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

  // Current query state from URL
  const currentPage = parseInt(searchParams.get("page") || "1");
  const currentSize = parseInt(searchParams.get("size") || "20");
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

  const SortIcon = ({ field }: { field: string }) => {
    if (currentSortBy !== field)
      return <ArrowUpDown className="size-3.5 text-slate-400" />;
    return currentOrder === "asc" ? (
      <ArrowUp className="size-3.5 text-slate-700" />
    ) : (
      <ArrowDown className="size-3.5 text-slate-700" />
    );
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

  return (
    <div className="space-y-4">
      {/* ── Search & Filter Toolbar ─────────────────────────── */}
      <Card className="border-none shadow-sm bg-white p-4">
        <div className="flex flex-col md:flex-row gap-3 items-center justify-between">
          {/* Search input */}
          <div className="relative w-full md:max-w-sm">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              <Search className="size-4" />
            </span>
            <Input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo tên hoặc slug..."
              className="pl-9 h-10 border-slate-200 focus-visible:ring-slate-400/20"
            />
          </div>

          <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
            {/* Status filter */}
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-slate-500 whitespace-nowrap">
                Trạng thái:
              </span>
              <select
                value={currentIsActive || ""}
                onChange={(e) =>
                  updateQueryParams({ isActive: e.target.value, page: 1 })
                }
                className="h-10 px-3 rounded-lg border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-slate-100 font-medium text-slate-700 cursor-pointer"
              >
                <option value="">Tất cả</option>
                <option value="true">Đang hoạt động</option>
                <option value="false">Đã ẩn</option>
              </select>
            </div>

            {/* Page size */}
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-slate-500 whitespace-nowrap">
                Hiển thị:
              </span>
              <select
                value={currentSize}
                onChange={(e) =>
                  updateQueryParams({ size: e.target.value, page: 1 })
                }
                className="h-10 px-3 rounded-lg border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-slate-100 font-medium text-slate-700 cursor-pointer"
              >
                <option value="10">10 / trang</option>
                <option value="20">20 / trang</option>
                <option value="50">50 / trang</option>
              </select>
            </div>

            {/* Add button */}
            <Button
              onClick={handleCreate}
              className="h-10 bg-slate-900 hover:bg-slate-800 text-white cursor-pointer gap-1.5 ml-auto"
            >
              <Plus className="size-4" />
              Thêm danh mục
            </Button>
          </div>
        </div>
      </Card>

      {/* ── Main Table Card ──────────────────────────────────── */}
      <Card className="border-none shadow-sm bg-white overflow-hidden">
        <div className="relative">
          {/* Loading overlay */}
          {isPending && (
            <div className="absolute inset-0 bg-white/60 backdrop-blur-xs flex items-center justify-center z-10">
              <div className="flex items-center gap-2 bg-slate-950 text-white px-4 py-2 rounded-full shadow-lg">
                <Loader2 className="size-4 animate-spin" />
                <span className="text-xs font-semibold">Đang cập nhật...</span>
              </div>
            </div>
          )}

          {initialData.content.length === 0 ? (
            /* Empty state */
            <CardContent className="flex flex-col items-center justify-center py-20 text-center">
              <div className="p-4 bg-slate-50 rounded-full text-slate-400 mb-4 border border-slate-100">
                <LayoutGrid className="size-12" />
              </div>
              <h3 className="text-lg font-bold text-slate-700">
                Không tìm thấy danh mục
              </h3>
              <p className="text-sm text-slate-400 mt-1 max-w-xs">
                Hãy thử thay đổi từ khóa hoặc bộ lọc, hoặc nhấn{" "}
                <span className="font-semibold text-slate-600">
                  Thêm danh mục
                </span>{" "}
                để tạo mới.
              </p>
            </CardContent>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-slate-50/70 border-b border-slate-100">
                  <TableRow>
                    <TableHead className="w-14 font-semibold text-slate-600 py-3.5">
                      Ảnh
                    </TableHead>
                    <TableHead
                      className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50 py-3.5"
                      onClick={() => handleSort("name")}
                    >
                      <div className="flex items-center gap-1.5">
                        Tên danh mục
                        <SortIcon field="name" />
                      </div>
                    </TableHead>
                    <TableHead
                      className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50"
                      onClick={() => handleSort("slug")}
                    >
                      <div className="flex items-center gap-1.5">
                        Slug
                        <SortIcon field="slug" />
                      </div>
                    </TableHead>
                    <TableHead className="font-semibold text-slate-600 text-center">
                      Trạng thái
                    </TableHead>
                    <TableHead className="font-semibold text-slate-600 text-center">
                      Thao tác
                    </TableHead>
                  </TableRow>
                </TableHeader>

                <TableBody>
                  {initialData.content.map((cat) => (
                    <TableRow
                      key={cat.id}
                      className="hover:bg-slate-50/50 border-b border-slate-100/80"
                    >
                      {/* Thumbnail */}
                      <TableCell>
                        {cat.thumbnail ? (
                          <img
                            src={cat.thumbnail}
                            alt={cat.name}
                            className="size-10 rounded-lg object-cover border border-slate-100"
                            onError={(e) => {
                              (e.target as HTMLImageElement).style.display =
                                "none";
                            }}
                          />
                        ) : (
                          <div className="size-10 rounded-lg bg-slate-100 flex items-center justify-center">
                            <ImageIcon className="size-4 text-slate-400" />
                          </div>
                        )}
                      </TableCell>

                      {/* Name */}
                      <TableCell className="font-semibold text-slate-800">
                        {cat.name}
                      </TableCell>

                      {/* Slug */}
                      <TableCell>
                        <span className="font-mono text-xs text-slate-500 bg-slate-50 border border-slate-100 px-2 py-0.5 rounded">
                          {cat.slug}
                        </span>
                      </TableCell>

                      {/* Status badge */}
                      <TableCell className="text-center">
                        {cat.active ? (
                          <Badge className="bg-emerald-50 hover:bg-emerald-50 text-emerald-600 border border-emerald-200 text-xs font-semibold px-2 py-0.5 shadow-none">
                            Hoạt động
                          </Badge>
                        ) : (
                          <Badge className="bg-slate-50 hover:bg-slate-50 text-slate-400 border border-slate-200 text-xs font-semibold px-2 py-0.5 shadow-none">
                            Đã ẩn
                          </Badge>
                        )}
                      </TableCell>

                      {/* Actions */}
                      <TableCell>
                        <div className="flex items-center justify-center gap-1.5">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="text-slate-500 hover:text-slate-800 hover:bg-slate-100 cursor-pointer"
                            onClick={() => handleEdit(cat)}
                          >
                            <Edit2 className="size-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="text-rose-400 hover:text-rose-600 hover:bg-rose-50 cursor-pointer"
                            onClick={() => {
                              setDeleteTarget(cat);
                              setIsDeleteOpen(true);
                            }}
                          >
                            <Trash2 className="size-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>

        {/* ── Pagination ───────────────────────────────────── */}
        {initialData.totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-4 bg-slate-50/50 border-t border-slate-100">
            <p className="text-sm text-slate-500 font-medium">
              Hiển thị{" "}
              <span className="font-bold text-slate-700">
                {initialData.numberOfElements}
              </span>{" "}
              trên tổng số{" "}
              <span className="font-bold text-slate-700">
                {initialData.totalElements}
              </span>{" "}
              danh mục.
            </p>

            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="icon"
                disabled={currentPage <= 1 || isPending}
                className="size-8.5 cursor-pointer border-slate-200 text-slate-600"
                onClick={() => updateQueryParams({ page: currentPage - 1 })}
              >
                <ChevronLeft className="size-4" />
              </Button>

              {Array.from(
                { length: initialData.totalPages },
                (_, i) => i + 1
              )
                .filter(
                  (p) =>
                    Math.abs(p - currentPage) <= 2 ||
                    p === 1 ||
                    p === initialData.totalPages
                )
                .map((p, idx, arr) => {
                  const showEllipsis = idx > 0 && p - arr[idx - 1] > 1;
                  return (
                    <div key={p} className="flex items-center">
                      {showEllipsis && (
                        <span className="px-1.5 text-slate-400 font-medium">
                          ...
                        </span>
                      )}
                      <Button
                        variant={p === currentPage ? "default" : "outline"}
                        size="sm"
                        disabled={isPending}
                        className={`size-8.5 text-xs font-semibold cursor-pointer ${
                          p === currentPage
                            ? "bg-slate-900 text-white shadow-sm"
                            : "border-slate-200 text-slate-600 hover:text-slate-800"
                        }`}
                        onClick={() => updateQueryParams({ page: p })}
                      >
                        {p}
                      </Button>
                    </div>
                  );
                })}

              <Button
                variant="outline"
                size="icon"
                disabled={currentPage >= initialData.totalPages || isPending}
                className="size-8.5 cursor-pointer border-slate-200 text-slate-600"
                onClick={() => updateQueryParams({ page: currentPage + 1 })}
              >
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* ── Create / Edit Dialog ─────────────────────────────── */}
      <CategoryFormDialog
        category={editingCategory}
        open={formOpen}
        onOpenChange={setFormOpen}
        onSuccess={() => router.refresh()}
      />

      {/* ── Delete Confirm Dialog ────────────────────────────── */}
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
