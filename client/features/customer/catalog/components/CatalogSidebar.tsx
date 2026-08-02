"use client";

import { useState, useEffect } from "react";
import { Search, SlidersHorizontal, Check } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { CatalogFilterParams, CategoryFacetData, AttributeFacet } from "@/features/customer/catalog/types/catalog.types";

interface CatalogSidebarProps {
  facets?: CategoryFacetData;
  filters: CatalogFilterParams;
  onUpdateFilters: (newParams: Partial<CatalogFilterParams>) => void;
}

export function CatalogSidebar({ facets, filters, onUpdateFilters }: CatalogSidebarProps) {
  const [openAccordionItems, setOpenAccordionItems] = useState<string[]>(["brands"]);

  useEffect(() => {
    if (facets?.attributes) {
      const attrCodes = facets.attributes.filter((a) => a.isFilterable).map((a) => a.code);
      setOpenAccordionItems(["brands", ...attrCodes]);
    }
  }, [facets]);
  const handleKeywordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onUpdateFilters({ search: e.target.value || undefined });
  };

  const handleBrandToggle = (brandId: number) => {
    const isSelected = filters.brandId === brandId;
    onUpdateFilters({ brandId: isSelected ? undefined : brandId });
  };

  const handleStockToggle = (checked: boolean) => {
    onUpdateFilters({ inStock: checked ? true : undefined });
  };

  const handleAttributeValueToggle = (attrCode: string, value: string) => {
    const currentMap = { ...(filters.attributes || {}) };
    const currentList = currentMap[attrCode] || [];

    let updatedList: string[];
    if (currentList.includes(value)) {
      updatedList = currentList.filter((v) => v !== value);
    } else {
      updatedList = [...currentList, value];
    }

    if (updatedList.length === 0) {
      delete currentMap[attrCode];
    } else {
      currentMap[attrCode] = updatedList;
    }

    onUpdateFilters({ attributes: currentMap });
  };

  const renderAttributeFilter = (attr: AttributeFacet) => {
    const currentValues = filters.attributes?.[attr.code] || [];

    // 1. Attribute has preset enumValues/options (ENUM, STRING, or NUMBER with choices)
    const options = attr.enumValues || [];
    if (options.length > 0) {
      return (
        <AccordionItem key={attr.code} value={attr.code} className="border-slate-150">
          <AccordionTrigger className="text-[11px] font-bold text-slate-700 hover:text-shop_light_green py-1.5">
            {attr.name} {attr.unit ? `(${attr.unit})` : ""}
          </AccordionTrigger>
          <AccordionContent>
            <div className="space-y-1 pt-0.5 max-h-40 overflow-y-auto pr-1">
              {options.map((option) => {
                const checked = currentValues.includes(option);
                return (
                  <div
                    key={option}
                    onClick={() => handleAttributeValueToggle(attr.code, option)}
                    className="flex items-center space-x-1.5 text-[11px] cursor-pointer hover:text-shop_light_green group py-0.5"
                  >
                    <div
                      className={`w-3.5 h-3.5 rounded border flex items-center justify-center transition-colors ${
                        checked
                          ? "bg-shop_light_green border-shop_light_green text-white"
                          : "border-slate-300 bg-white group-hover:border-slate-400"
                      }`}
                    >
                      {checked && <Check className="w-2.5 h-2.5 stroke-[3]" />}
                    </div>
                    <span className={checked ? "text-shop_light_green font-semibold" : "text-slate-600"}>
                      {option} {attr.unit && !option.endsWith(attr.unit) ? attr.unit : ""}
                    </span>
                  </div>
                );
              })}
            </div>
          </AccordionContent>
        </AccordionItem>
      );
    }

    // 2. BOOLEAN type
    if (attr.dataType === "BOOLEAN") {
      const checked = currentValues.includes("true");
      return (
        <div key={attr.code} className="flex items-center justify-between py-1.5 border-b border-slate-150 text-[11px]">
          <span className="text-slate-700 font-medium">{attr.name}</span>
          <Checkbox
            checked={checked}
            onCheckedChange={(c) => handleAttributeValueToggle(attr.code, "true")}
            className="data-[state=checked]:bg-shop_light_green border-slate-300 w-3.5 h-3.5"
          />
        </div>
      );
    }

    // 3. NUMBER / INTEGER / DECIMAL type without enumValues (Min - Max inputs)
    if (attr.dataType === "NUMBER" || attr.minBound !== undefined || attr.maxBound !== undefined) {
      const currentMin = currentValues[0] || "";
      const currentMax = currentValues[1] || "";

      return (
        <AccordionItem key={attr.code} value={attr.code} className="border-slate-150">
          <AccordionTrigger className="text-[11px] font-bold text-slate-700 hover:text-shop_light_green py-1.5">
            {attr.name} {attr.unit ? `(${attr.unit})` : ""}
          </AccordionTrigger>
          <AccordionContent>
            <div className="flex items-center gap-1.5 pt-1">
              <Input
                type="number"
                placeholder={attr.minBound !== undefined ? `Từ ${attr.minBound}` : "Min"}
                value={currentMin}
                onChange={(e) => {
                  const val = e.target.value;
                  const map = { ...(filters.attributes || {}) };
                  if (!val && !currentMax) delete map[attr.code];
                  else map[attr.code] = [val, currentMax];
                  onUpdateFilters({ attributes: map });
                }}
                className="h-7 text-[11px] px-2 bg-slate-50 border-slate-200"
              />
              <span className="text-slate-400 text-xs">-</span>
              <Input
                type="number"
                placeholder={attr.maxBound !== undefined ? `Đến ${attr.maxBound}` : "Max"}
                value={currentMax}
                onChange={(e) => {
                  const val = e.target.value;
                  const map = { ...(filters.attributes || {}) };
                  if (!currentMin && !val) delete map[attr.code];
                  else map[attr.code] = [currentMin, val];
                  onUpdateFilters({ attributes: map });
                }}
                className="h-7 text-[11px] px-2 bg-slate-50 border-slate-200"
              />
            </div>
          </AccordionContent>
        </AccordionItem>
      );
    }

    // 4. Freeform STRING / Text filter
    return (
      <AccordionItem key={attr.code} value={attr.code} className="border-slate-150">
        <AccordionTrigger className="text-[11px] font-bold text-slate-700 hover:text-shop_light_green py-1.5">
          {attr.name}
        </AccordionTrigger>
        <AccordionContent>
          <Input
            type="text"
            placeholder={`Nhập ${attr.name.toLowerCase()}...`}
            value={currentValues[0] || ""}
            onChange={(e) => {
              const val = e.target.value;
              const map = { ...(filters.attributes || {}) };
              if (!val) delete map[attr.code];
              else map[attr.code] = [val];
              onUpdateFilters({ attributes: map });
            }}
            className="h-7 text-[11px] px-2 bg-slate-50 border-slate-200"
          />
        </AccordionContent>
      </AccordionItem>
    );
  };

  return (
    <aside className="w-full lg:w-56 flex-shrink-0 bg-white border border-slate-200/80 rounded-xl p-3 shadow-2xs">
      <div className="flex items-center gap-1.5 mb-2.5 pb-2 border-b border-slate-100 text-shop_light_green font-bold text-xs">
        <SlidersHorizontal className="w-3.5 h-3.5" />
        <span>Bộ lọc kỹ thuật</span>
      </div>

      {/* Keyword Search */}
      <div className="relative mb-2.5">
        <Search className="w-3 h-3 absolute left-2.5 top-2 text-slate-400" />
        <Input
          type="text"
          placeholder="Tìm tên hoặc mô tả..."
          value={filters.search || ""}
          onChange={handleKeywordChange}
          className="pl-7 h-7 text-[11px] bg-slate-50 border-slate-200 focus:border-shop_light_green text-slate-800 rounded-md"
        />
      </div>

      {/* In Stock Filter */}
      <div className="flex items-center justify-between py-1.5 px-2 bg-slate-50 rounded-md border border-slate-150 mb-2.5 text-[11px]">
        <Label htmlFor="inStock" className="text-slate-700 font-medium cursor-pointer">
          Chỉ hiện sản phẩm còn hàng
        </Label>
        <Checkbox
          id="inStock"
          checked={Boolean(filters.inStock)}
          onCheckedChange={handleStockToggle}
          className="data-[state=checked]:bg-shop_light_green border-slate-300 w-3.5 h-3.5"
        />
      </div>

      <Accordion value={openAccordionItems} onValueChange={setOpenAccordionItems}>
        {/* Brands Section */}
        {facets?.brands && facets.brands.length > 0 && (
          <AccordionItem value="brands" className="border-slate-150">
            <AccordionTrigger className="text-[11px] font-bold text-slate-700 hover:text-shop_light_green py-1.5">
              Thương hiệu
            </AccordionTrigger>
            <AccordionContent>
              <div className="space-y-1 pt-0.5 max-h-40 overflow-y-auto pr-1">
                {facets.brands.map((brand) => {
                  const checked = filters.brandId === brand.id;
                  return (
                    <div
                      key={brand.id}
                      onClick={() => handleBrandToggle(brand.id)}
                      className="flex items-center space-x-1.5 text-[11px] cursor-pointer hover:text-shop_light_green group py-0.5"
                    >
                      <div
                        className={`w-3.5 h-3.5 rounded border flex items-center justify-center transition-colors ${
                          checked
                            ? "bg-shop_light_green border-shop_light_green text-white"
                            : "border-slate-300 bg-white group-hover:border-slate-400"
                        }`}
                      >
                        {checked && <Check className="w-2.5 h-2.5 stroke-[3]" />}
                      </div>
                      <span className={checked ? "text-shop_light_green font-semibold" : "text-slate-600"}>
                        {brand.name}
                      </span>
                    </div>
                  );
                })}
              </div>
            </AccordionContent>
          </AccordionItem>
        )}

        {/* Dynamic Filterable Attributes */}
        {facets?.attributes
          .filter((a) => a.isFilterable)
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map(renderAttributeFilter)
          .filter(Boolean)}
      </Accordion>
    </aside>
  );
}
