"use client";

import { AttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { Loader2, Trash2 } from "lucide-react";

interface AttributeListProps {
  attributes: AttributeResponse[];
  isLoading: boolean;
  isPending: boolean;
  onDelete: (id: number) => void;
}

export default function AttributeList({
  attributes,
  isLoading,
  isPending,
  onDelete,
}: AttributeListProps) {
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-6 text-slate-400 gap-1.5">
        <Loader2 className="size-4 animate-spin" />
        <span className="text-xs font-medium">Đang tải...</span>
      </div>
    );
  }

  if (attributes.length === 0) {
    return (
      <div className="text-center py-8 bg-white border border-dashed border-slate-200 rounded-xl">
        <p className="text-xs font-medium text-slate-400">
          Chưa có thuộc tính nào.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2 max-h-[220px] overflow-y-auto pr-1">
      {attributes.map((attr) => (
        <div
          key={attr.id}
          className="flex items-center justify-between p-3 bg-white border border-slate-150 rounded-xl shadow-xs"
        >
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="text-xs font-bold text-slate-800">
                {attr.name}
              </span>
            </div>

            <div className="flex items-center gap-1 flex-wrap">
              <span className="text-[10px] text-slate-500 font-mono">
                {attr.code}
              </span>
              <span className="text-[10px] text-slate-300">•</span>
              <span className="text-[10px] text-slate-500 font-bold">
                {attr.dataType}
              </span>
              {attr.dataType === "NUMBER" && attr.unit && (
                <span className="text-[11px] text-slate-500 font-medium">
                  ({attr.unit})
                </span>
              )}
              {attr.dataType === "ENUM" && attr.enumValues && (
                <span
                  className="text-[10px] text-slate-500 bg-slate-100 px-1.5 py-0.5 rounded-md font-medium truncate max-w-[180px]"
                  title={
                    Array.isArray(attr.enumValues)
                      ? attr.enumValues.join(", ")
                      : Object.keys(attr.enumValues).join(", ")
                  }
                >
                  {Array.isArray(attr.enumValues)
                    ? attr.enumValues.join(", ")
                    : Object.keys(attr.enumValues).join(", ")}
                </span>
              )}
            </div>
          </div>

          <button
            onClick={() => onDelete(attr.id)}
            disabled={isPending}
            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors cursor-pointer disabled:opacity-50"
          >
            <Trash2 className="size-4" />
          </button>
        </div>
      ))}
    </div>
  );
}
