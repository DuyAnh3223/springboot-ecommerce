import React from 'react';
import { CategoryResponse } from "../category.type";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CardContent } from "@/components/ui/card";
import {
  Edit2,
  Trash2,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  LayoutGrid,
  Loader2,
  Image as ImageIcon,
  Sliders,
} from "lucide-react";

interface DataTableProps {
  data: CategoryResponse[];
  isPending: boolean;
  currentSortBy: string;
  currentOrder: string;
  onSort: (field: string) => void;
  onEdit: (category: CategoryResponse) => void;
  onDelete: (category: CategoryResponse) => void;
  onManageAttributes?: (category: CategoryResponse) => void;
}

const DataTable = ({
  data,
  isPending,
  currentSortBy,
  currentOrder,
  onSort,
  onEdit,
  onDelete,
  onManageAttributes,
}: DataTableProps) => {
  const SortIcon = ({ field }: { field: string }) => {
    if (currentSortBy !== field)
      return <ArrowUpDown className="size-3 text-slate-400" />;
    return currentOrder === "asc" ? (
      <ArrowUp className="size-3 text-slate-700" />
    ) : (
      <ArrowDown className="size-3 text-slate-700" />
    );
  };

  return (
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

      {data.length === 0 ? (
        /* Empty state */
        <CardContent className="flex flex-col items-center justify-center py-20 text-center">
          <div className="p-4 bg-slate-50 rounded-full text-slate-400 mb-4 border border-slate-100">
            <LayoutGrid className="size-12" />
          </div>
          <h3 className="text-lg font-bold text-slate-700">
            Không tìm thấy danh mục
          </h3>
          <p className="text-sm text-slate-400 mt-1 max-w-xs">
            Hãy thử thay đổi từ khóa hoặc bộ lọc để hiển thị kết quả.
          </p>
        </CardContent>
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader className="bg-slate-50/50 border-b border-slate-100">
              <TableRow className="hover:bg-transparent">
                <TableHead className="w-12 py-2.5 pl-4">
                  <input
                    type="checkbox"
                    className="rounded border-slate-300 accent-shop_dark_green cursor-pointer size-4"
                    disabled
                  />
                </TableHead>
                {/* <TableHead className="w-14 font-semibold text-slate-600 py-2.5">
                  Ảnh
                </TableHead> */}
                <TableHead
                  className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50 py-2.5"
                  onClick={() => onSort("name")}
                >
                  <div className="flex items-center gap-1.5">
                    Tên danh mục
                    <SortIcon field="name" />
                  </div>
                </TableHead>
                
                <TableHead className="font-semibold text-slate-600 text-center py-2.5">
                  Trạng thái
                </TableHead>
                <TableHead className="font-semibold text-slate-600 text-center py-2.5 w-24">
                  Thao tác
                </TableHead>
              </TableRow>
            </TableHeader>

            <TableBody>
              {data.map((cat) => (
                <TableRow
                  key={cat.id}
                  className="hover:bg-slate-50/30 border-b border-slate-100/80 transition-colors"
                >
                  {/* Checkbox column */}
                  <TableCell className="py-2 pl-4">
                    <input
                      type="checkbox"
                      className="rounded border-slate-350 accent-shop_dark_green cursor-pointer size-4"
                    />
                  </TableCell>

                  {/* Thumbnail, Name & Slug */}
                  <TableCell className="py-2 text-sm">
                    <div className="flex items-center gap-3">
                      {/* Image Thumbnail */}
                      <div className="shrink-0">
                        {cat.thumbnail ? (
                          <img
                            src={cat.thumbnail}
                            alt={cat.name}
                            className="size-8.5 rounded-md object-cover border border-slate-100 shadow-xs"
                            onError={(e) => {
                              (e.target as HTMLImageElement).style.display =
                                "none";
                            }}
                          />
                        ) : (
                          <div className="size-8.5 rounded-md bg-slate-100 flex items-center justify-center border border-slate-100">
                            <ImageIcon className="size-3.5 text-slate-400" />
                          </div>
                        )}
                      </div>

                      {/* Name & Slug */}
                      <div className="flex flex-col gap-0.5">
                        <div className="font-semibold text-slate-950 leading-tight">
                          {cat.name}
                        </div>
                        <div className="font-mono text-[10px] text-slate-450 font-normal leading-tight">
                          {cat.slug}
                        </div>
                      </div>
                    </div>
                  </TableCell>

                  {/* Status badge */}
                  <TableCell className="text-center py-2">
                    {cat.active ? (
                      <Badge className="bg-emerald-50 hover:bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-semibold px-2 py-0.5 shadow-none rounded-md">
                        Active
                      </Badge>
                    ) : (
                      <Badge className="bg-slate-50 hover:bg-slate-50 text-slate-500 border border-slate-200 text-xs font-semibold px-2 py-0.5 shadow-none rounded-md">
                        Draft
                      </Badge>
                    )}
                  </TableCell>

                  {/* Actions */}
                  <TableCell className="py-2">
                    <div className="flex items-center justify-center gap-1">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-slate-500 hover:text-slate-800 hover:bg-slate-100 cursor-pointer size-8"
                        onClick={() => onEdit(cat)}
                      >
                        <Edit2 className="size-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-slate-500 hover:text-slate-800 hover:bg-slate-100 cursor-pointer size-8"
                        title="Quản lý thuộc tính"
                        onClick={() => onManageAttributes?.(cat)}
                      >
                        <Sliders className="size-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        className="text-rose-450 hover:text-rose-600 hover:bg-rose-50 cursor-pointer size-8"
                        onClick={() => onDelete(cat)}
                      >
                        <Trash2 className="size-3.5" />
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
  );
};

export default DataTable;