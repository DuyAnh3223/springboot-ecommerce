"use client";

import { AttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ChevronUp,
  ChevronDown,
  Edit2,
  Trash2,
  Loader2,
} from "lucide-react";

interface AttributesTableProps {
  attributes: AttributeResponse[];
  isPending: boolean;
  currentSortBy: string;
  currentOrder: "asc" | "desc";
  onSort: (field: string) => void;
  onEdit: (attr: AttributeResponse) => void;
  onDelete: (attr: AttributeResponse) => void;
}

export default function AttributesTable({
  attributes,
  isPending,
  currentSortBy,
  currentOrder,
  onSort,
  onEdit,
  onDelete,
}: AttributesTableProps) {
  return (
    <Table>
      <TableHeader className="bg-slate-50/70 border-b border-slate-100">
        <TableRow>
          <TableHead
            onClick={() => onSort("name")}
            className="cursor-pointer select-none text-xs font-bold text-slate-650 h-10 w-[20%]"
          >
            Tên thuộc tính
            {currentSortBy === "name" && (
              currentOrder === "asc" ? <ChevronUp className="inline ml-1 size-3" /> : <ChevronDown className="inline ml-1 size-3" />
            )}
          </TableHead>
          <TableHead
            onClick={() => onSort("code")}
            className="cursor-pointer select-none text-xs font-bold text-slate-650 h-10 w-[20%]"
          >
            Mã thuộc tính
            {currentSortBy === "code" && (
              currentOrder === "asc" ? <ChevronUp className="inline ml-1 size-3" /> : <ChevronDown className="inline ml-1 size-3" />
            )}
          </TableHead>
          <TableHead className="text-xs font-bold text-slate-650 h-10 w-[12%]">Kiểu dữ liệu</TableHead>
          <TableHead className="text-xs font-bold text-slate-650 h-10 w-[8%]">Đơn vị</TableHead>
          <TableHead className="text-xs font-bold text-slate-650 h-10 w-[25%]">Giá trị gợi ý (Enum)</TableHead>
          <TableHead className="text-xs font-bold text-slate-650 h-10 w-[15%] text-right pr-6">Hành động</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {isPending && attributes.length === 0 ? (
          <TableRow>
            <TableCell colSpan={6} className="text-center py-10">
              <Loader2 className="size-6 animate-spin mx-auto text-slate-400 mb-2" />
              <span className="text-xs text-slate-500 font-medium">Đang tải dữ liệu...</span>
            </TableCell>
          </TableRow>
        ) : attributes.length === 0 ? (
          <TableRow>
            <TableCell colSpan={6} className="text-center py-12">
              <p className="text-xs font-semibold text-slate-400">Không tìm thấy thuộc tính nào.</p>
            </TableCell>
          </TableRow>
        ) : (
          attributes.map((attr) => (
            <TableRow key={attr.id} className="hover:bg-slate-50/50 border-b border-slate-100">
              <TableCell className="font-semibold text-slate-900 text-xs py-3">{attr.name}</TableCell>
              <TableCell className="font-mono text-slate-500 text-xs py-3">{attr.code}</TableCell>
              <TableCell className="py-3">
                <Badge variant="outline" className="text-[10px] font-bold px-2 bg-slate-100/50 text-slate-600 border-slate-200">
                  {attr.dataType}
                </Badge>
              </TableCell>
              <TableCell className="text-xs text-slate-655 font-medium py-3">{attr.unit || "-"}</TableCell>
              <TableCell className="py-3 max-w-[280px]">
                {attr.dataType === "ENUM" && attr.enumValues ? (
                  <div className="flex flex-wrap gap-1 max-h-[48px] overflow-y-auto pr-1">
                    {(Array.isArray(attr.enumValues) ? attr.enumValues : Object.keys(attr.enumValues)).map((val) => (
                      <Badge key={val} className="text-[9px] bg-slate-100 text-slate-600 border border-slate-200 font-medium">
                        {val}
                      </Badge>
                    ))}
                  </div>
                ) : (
                  <span className="text-xs text-slate-400 font-medium">-</span>
                )}
              </TableCell>
              <TableCell className="text-right pr-6 py-3 space-x-1.5">
                <Button
                  variant="outline"
                  onClick={() => onEdit(attr)}
                  className="p-1 h-7 w-7 text-slate-500 hover:text-slate-800 border-slate-200 cursor-pointer rounded-lg inline-flex items-center justify-center"
                  title="Sửa thuộc tính"
                >
                  <Edit2 className="size-3.5" />
                </Button>
                <Button
                  variant="outline"
                  onClick={() => onDelete(attr)}
                  className="p-1 h-7 w-7 text-slate-400 hover:text-rose-600 hover:bg-rose-50 border-slate-200 cursor-pointer rounded-lg inline-flex items-center justify-center"
                  title="Xóa thuộc tính"
                >
                  <Trash2 className="size-3.5" />
                </Button>
              </TableCell>
            </TableRow>
          ))
        )}
      </TableBody>
    </Table>
  );
}
