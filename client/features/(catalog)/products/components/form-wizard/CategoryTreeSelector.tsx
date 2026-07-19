"use client";

import React, { useState, useMemo } from "react";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import { Command, CommandList, CommandEmpty, CommandGroup, CommandItem } from "@/components/ui/command";
import { Badge } from "@/components/ui/badge";
import { ChevronDown, Search } from "lucide-react";

interface CategoryTreeSelectorProps {
  categories: CategoryResponse[];
  selectedCategoryId: number;
  onSelect: (categoryId: number) => void;
  isEdit: boolean;
  setError: (err: string | null) => void;
}

export function CategoryTreeSelector({
  categories,
  selectedCategoryId,
  onSelect,
  isEdit,
  setError,
}: CategoryTreeSelectorProps) {
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [categorySearch, setCategorySearch] = useState("");

  // Build flattened categories with paths for tree-search Popover
  const categoryTreeOptions = useMemo(() => {
    const map: Record<number, CategoryResponse> = {};
    categories.forEach((c) => (map[c.id] = c));

    const getPath = (id: number): string[] => {
      const pathList: string[] = [];
      let curr: CategoryResponse | undefined = map[id];
      while (curr) {
        pathList.unshift(curr.name);
        curr = curr.parentId ? map[curr.parentId] : undefined;
      }
      return pathList;
    };

    return categories.map((c) => {
      const path = getPath(c.id);
      const isLeafNode = !categories.some((child) => child.parentId === c.id);
      return {
        id: c.id,
        name: c.name,
        fullPath: path.join("  ›  "),
        isLeaf: isLeafNode,
      };
    });
  }, [categories]);

  const selectedPath = useMemo(() => {
    if (!selectedCategoryId) return "Tìm kiếm & chọn danh mục...";
    const opt = categoryTreeOptions.find((o) => o.id === selectedCategoryId);
    return opt ? opt.fullPath : "Chọn danh mục";
  }, [selectedCategoryId, categoryTreeOptions]);

  const handleSelect = (id: number) => {
    const opt = categoryTreeOptions.find((o) => o.id === id);
    if (!opt) return;

    if (!opt.isLeaf) {
      setError("Vui lòng chọn danh mục cấp cuối cùng (danh mục lá).");
      return;
    }

    setError(null);
    onSelect(id);
    setCategoryOpen(false);
  };

  const filteredOptions = useMemo(() => {
    return categoryTreeOptions.filter((opt) =>
      opt.fullPath.toLowerCase().includes(categorySearch.toLowerCase())
    );
  }, [categoryTreeOptions, categorySearch]);

  return (
    <Popover open={categoryOpen} onOpenChange={setCategoryOpen}>
      <PopoverTrigger
        render={
          <Button
            type="button"
            variant="outline"
            role="combobox"
            aria-expanded={categoryOpen}
            className="w-full justify-between border-slate-200 h-10 text-slate-700 bg-white font-normal text-xs"
            disabled={isEdit}
          />
        }
      >
        {selectedPath}
        <ChevronDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
      </PopoverTrigger>
      <PopoverContent className="w-[450px] p-0 bg-white shadow-xl rounded-xl border border-slate-100" align="start">
        <Command className="rounded-xl" shouldFilter={false}>
          <div className="flex items-center px-3 border-b border-slate-100">
            <Search className="mr-2 h-4 w-4 shrink-0 opacity-50" />
            <input
              placeholder="Gõ để tìm kiếm nhanh danh mục..."
              value={categorySearch}
              onChange={(e) => setCategorySearch(e.target.value)}
              className="flex h-11 w-full rounded-md bg-transparent py-3 text-xs outline-none placeholder:text-slate-400 text-slate-800"
            />
          </div>
          <CommandList className="max-h-[300px] overflow-y-auto p-1.5">
            <CommandEmpty className="py-6 text-center text-xs text-slate-400">
              Không tìm thấy danh mục nào.
            </CommandEmpty>
            <CommandGroup>
              {filteredOptions.map((opt) => (
                <CommandItem
                  key={opt.id}
                  value={String(opt.id)}
                  onSelect={() => handleSelect(opt.id)}
                  className={`flex justify-between items-center text-2xs p-2 rounded-lg cursor-pointer ${
                    opt.isLeaf
                      ? "text-slate-700 hover:bg-slate-50"
                      : "text-slate-400 opacity-60 bg-slate-50/30"
                  }`}
                >
                  <span className={opt.isLeaf ? "font-semibold" : "font-normal"}>
                    {opt.fullPath}
                  </span>
                  {!opt.isLeaf && (
                    <Badge variant="outline" className="text-[9px] border-slate-200 py-0 text-slate-400 font-normal">
                      Danh mục cha
                    </Badge>
                  )}
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
