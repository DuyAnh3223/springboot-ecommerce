"use client";

import { useEffect, useState, useTransition } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import { AttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { PageResponse } from "@/types/page.type";
import { deleteAttributeAction } from "@/features/(catalog)/attributes/actions";
import AttributesTable from "./AttributesTable";
import AttributeFormDialog from "./AttributeFormDialog";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Sliders,
  Plus,
  Search,
  Trash2,
  AlertCircle,
  Loader2,
} from "lucide-react";
import PaginationBar from "@/features/(catalog)/categories/components/PaginationBar";

interface AttributesClientProps {
  initialData: PageResponse<AttributeResponse>;
}

export default function AttributesClient({ initialData }: AttributesClientProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();

  // Search & Filters
  const [searchTerm, setSearchTerm] = useState(searchParams.get("keyword") || "");
  const currentPage = parseInt(searchParams.get("page") || "1");
  const currentSize = parseInt(searchParams.get("size") || "10");
  const currentSortBy = searchParams.get("sortBy") || "name";
  const currentOrder = (searchParams.get("order") as "asc" | "desc") || "asc";

  // Form states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingAttr, setEditingAttr] = useState<AttributeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Delete State
  const [deleteTarget, setDeleteTarget] = useState<AttributeResponse | null>(null);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  // Search debounce
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
    const nextOrder = currentSortBy === field && currentOrder === "asc" ? "desc" : "asc";
    updateQueryParams({ sortBy: field, order: nextOrder, page: 1 });
  };

  const handleOpenCreate = () => {
    setEditingAttr(null);
    setDialogOpen(true);
  };

  const handleOpenEdit = (attr: AttributeResponse) => {
    setEditingAttr(attr);
    setDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setError(null);
    startTransition(async () => {
      const result = await deleteAttributeAction(deleteTarget.id);
      if (result.error) {
        setError(result.error);
      } else {
        setIsDeleteOpen(false);
        setDeleteTarget(null);
        router.refresh();
      }
    });
  };

  const handleSaveSuccess = () => {
    router.refresh();
  };

  return (
    <div className="space-y-4 w-full">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <h1 className="text-xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Sliders className="size-5 text-shop_light_green" /> Quản lý thuộc tính 
          </h1>
          <p className="text-xs text-slate-500 font-medium">
            Định nghĩa các thuộc tính lõi và kiểu dữ liệu dùng chung trong toàn bộ hệ thống.
          </p>
        </div>
        <Button
          onClick={handleOpenCreate}
          className="bg-shop_dark_green hover:bg-shop_btn_dark_green text-white text-xs font-bold px-3 h-9 rounded-lg shadow-sm gap-1.5 cursor-pointer"
        >
          <Plus className="size-4" /> Định nghĩa thuộc tính
        </Button>
      </div>

      {/* Toolbar */}
      <div className="flex items-center justify-between gap-3 bg-white p-3 border border-slate-100 rounded-xl shadow-xs">
        <div className="relative max-w-sm flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 size-4" />
          <Input
            placeholder="Tìm theo tên hoặc mã thuộc tính..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 h-9 border-slate-200 focus-visible:ring-slate-350/50 text-xs bg-slate-50/50"
          />
        </div>
      </div>

      {/* Attributes Table Card */}
      <Card className="border border-slate-100 shadow-xs bg-white overflow-hidden">
        <AttributesTable
          attributes={initialData.content}
          isPending={isPending}
          currentSortBy={currentSortBy}
          currentOrder={currentOrder}
          onSort={handleSort}
          onEdit={handleOpenEdit}
          onDelete={(attr) => {
            setDeleteTarget(attr);
            setIsDeleteOpen(true);
          }}
        />
      </Card>

      {/* Pagination */}
      <PaginationBar
        initialData={initialData}
        currentPage={currentPage}
        currentSize={currentSize}
        isPending={isPending}
        onPageChange={(p) => updateQueryParams({ page: p })}
        onSizeChange={(val) => updateQueryParams({ size: val, page: 1 })}
      />

      {/* Edit / Create Form Dialog */}
      <AttributeFormDialog
        editingAttr={editingAttr}
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSuccess={handleSaveSuccess}
      />

      {/* Delete Confirm Dialog */}
      <Dialog open={isDeleteOpen} onOpenChange={setIsDeleteOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-rose-600 flex items-center gap-2">
              <Trash2 className="size-5" /> Xóa thuộc tính lõi
            </DialogTitle>
            <DialogDescription className="pt-2">
              Bạn có chắc chắn muốn xóa định nghĩa thuộc tính{" "}
              <span className="font-bold text-slate-800">
                &quot;{deleteTarget?.name}&quot; ({deleteTarget?.code})
              </span>
              ? Hành động này sẽ xóa vĩnh viễn định nghĩa thuộc tính khỏi kho Master Data.
            </DialogDescription>
          </DialogHeader>

          {error && (
            <div className="flex items-center gap-2 p-3 text-xs text-destructive bg-destructive/10 border border-destructive/20 rounded-lg">
              <AlertCircle className="size-4 shrink-0" />
              <p className="font-semibold">{error}</p>
            </div>
          )}

          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={() => setIsDeleteOpen(false)}
              className="cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button
              onClick={handleDeleteConfirm}
              disabled={isPending}
              className="bg-rose-600 hover:bg-rose-700 text-white cursor-pointer"
            >
              {isPending ? (
                <>
                  <Loader2 className="mr-1.5 h-3.5 w-3.5 animate-spin" />
                  Đang xóa...
                </>
              ) : (
                "Đồng ý xóa"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
