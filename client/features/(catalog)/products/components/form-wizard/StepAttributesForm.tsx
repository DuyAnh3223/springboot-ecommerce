"use client";

import React from "react";
import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Layers, Settings2, Sliders, Save, Loader2, ArrowRight } from "lucide-react";

interface StepAttributesFormProps {
  selectedCategoryName: string;
  categoryAttributes: CategoryAttributeResponse[];
  loadingAttributes: boolean;
  nonVariantValues: Record<string, any>;
  setNonVariantValues: (val: Record<string, any>) => void;
  selectedVariants: Record<string, string[]>;
  setSelectedVariants: (val: Record<string, string[]>) => void;
  newCustomTag: Record<string, string>;
  setNewCustomTag: (val: Record<string, string>) => void;
  scrollToSection: (step: number) => void;
}

export function StepAttributesForm({
  selectedCategoryName,
  categoryAttributes,
  loadingAttributes,
  nonVariantValues,
  setNonVariantValues,
  selectedVariants,
  setSelectedVariants,
  newCustomTag,
  setNewCustomTag,
  scrollToSection,
}: StepAttributesFormProps) {
  const handleAddCustomTag = (attrCode: string) => {
    const val = newCustomTag[attrCode]?.trim();
    if (!val) return;

    const currentList = selectedVariants[attrCode] || [];
    if (!currentList.includes(val)) {
      setSelectedVariants({
        ...selectedVariants,
        [attrCode]: [...currentList, val],
      });
    }
    setNewCustomTag({ ...newCustomTag, [attrCode]: "" });
  };

  return (
    <Card className="border-none shadow-sm bg-white overflow-hidden">
      <CardHeader className="bg-slate-50/50 border-b border-slate-100 pr-6">
        <div>
          <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
            <Sliders className="size-4.5 text-shop_dark_green" /> Bước 2: Nhập Thuộc Tính (EAV Attributes)
          </CardTitle>
          <CardDescription className="text-xs text-slate-450">
            Thiết lập giá trị cho các thuộc tính chung (A) và thuộc tính biến thể tạo SKU (B) tương ứng với danh mục{" "}
            <strong className="text-slate-700 font-bold">{selectedCategoryName}</strong>.
          </CardDescription>
        </div>
      </CardHeader>
      <CardContent className="p-6 space-y-8">
        {loadingAttributes ? (
          <div className="flex flex-col items-center justify-center py-12 text-slate-400">
            <Loader2 className="size-8 animate-spin mb-2 text-shop_dark_green" />
            <p className="text-xs">Đang tải danh sách thuộc tính cho danh mục...</p>
          </div>
        ) : categoryAttributes.length === 0 ? (
          <div className="text-center py-8 text-slate-450 bg-slate-50 border border-slate-150 rounded-xl flex flex-col items-center justify-center">
            <Layers className="size-8 text-slate-300 mb-2" />
            <p className="text-xs font-semibold">Danh mục này chưa được gán thuộc tính EAV nào.</p>
            <p className="text-[10px] text-slate-400 mt-1">Vui lòng click "Tiếp tục" bên dưới để điền SKU mặc định.</p>
            <Button
              type="button"
              onClick={() => scrollToSection(3)}
              className="bg-shop_dark_green hover:bg-shop_dark_green/90 text-white cursor-pointer text-2xs font-bold mt-4"
            >
              Tiếp tục đến SKU Matrix <ArrowRight className="size-3.5 ml-1" />
            </Button>
          </div>
        ) : (
          <div className="space-y-8">
            {/* Area A: Common attributes (isVariantDefining: false) */}
            <div className="space-y-4">
              <h3 className="text-xs font-bold text-slate-800 flex items-center gap-1.5 pb-2 border-b border-slate-100">
                <Layers className="size-4 text-shop_dark_green" /> Khu vực A: Thuộc tính chung (Common Attributes)
              </h3>
              {categoryAttributes.filter((ca) => !ca.isVariantDefining).length === 0 ? (
                <p className="text-[10px] text-slate-400 italic">Không có thuộc tính chung nào.</p>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {categoryAttributes
                    .filter((ca) => !ca.isVariantDefining)
                    .map((ca) => (
                      <div key={ca.id} className="space-y-2">
                        <Label className="text-slate-700 font-bold text-xs">
                          {ca.name} {ca.unit ? `(${ca.unit})` : ""}{" "}
                          {ca.isRequired && <span className="text-red-500">*</span>}
                        </Label>

                        {ca.isMultiValue && ca.enumValues && ca.enumValues.length > 0 ? (
                          <div className="flex flex-wrap gap-1.5 mt-1">
                            {(ca.enumValues || []).map((val: any) => {
                              const strVal = typeof val === "string" ? val : val.value;
                              const currentList = Array.isArray(nonVariantValues[ca.code])
                                ? nonVariantValues[ca.code]
                                : typeof nonVariantValues[ca.code] === "string"
                                ? nonVariantValues[ca.code]
                                    .split(",")
                                    .map((s: string) => s.trim())
                                    .filter(Boolean)
                                : [];
                              const isChecked = currentList.includes(strVal);
                              return (
                                <button
                                  key={strVal}
                                  type="button"
                                  onClick={() => {
                                    const newList = isChecked
                                      ? currentList.filter((v: string) => v !== strVal)
                                      : [...currentList, strVal];
                                    setNonVariantValues({
                                      ...nonVariantValues,
                                      [ca.code]: newList,
                                    });
                                  }}
                                  className={`px-2.5 py-1 rounded-md text-[10px] font-bold border transition-all cursor-pointer ${
                                    isChecked
                                      ? "bg-shop_dark_green text-white border-shop_dark_green shadow-2xs"
                                      : "bg-white text-slate-655 border-slate-200 hover:bg-slate-50"
                                  }`}
                                >
                                  {strVal}
                                </button>
                              );
                            })}
                          </div>
                        ) : ca.isMultiValue ? (
                          <div>
                            <Input
                              type="text"
                              placeholder="Ví dụ: Giá trị 1, Giá trị 2..."
                              value={
                                Array.isArray(nonVariantValues[ca.code])
                                  ? nonVariantValues[ca.code].join(", ")
                                  : nonVariantValues[ca.code] || ""
                              }
                              onChange={(e) =>
                                setNonVariantValues({
                                  ...nonVariantValues,
                                  [ca.code]: e.target.value,
                                })
                              }
                              className="border-slate-200 h-9 text-xs focus-visible:ring-shop_dark_green/10"
                            />
                            <span className="text-[9px] text-slate-400 mt-1 block">
                              Ngăn cách các giá trị bằng dấu phẩy.
                            </span>
                          </div>
                        ) : ca.dataType === "ENUM" || (ca.enumValues && ca.enumValues.length > 0) ? (
                          <select
                            value={nonVariantValues[ca.code] || ""}
                            onChange={(e) =>
                              setNonVariantValues({
                                ...nonVariantValues,
                                [ca.code]: e.target.value,
                              })
                            }
                            className="h-9 w-full border border-slate-200 rounded-md px-3 text-xs text-slate-700 bg-white focus:outline-none focus:border-slate-400"
                          >
                            <option value="">Chọn giá trị</option>
                            {(ca.enumValues || []).map((val: any) => {
                              const strVal = typeof val === "string" ? val : val.value;
                              return (
                                <option key={strVal} value={strVal}>
                                  {strVal}
                                </option>
                              );
                            })}
                          </select>
                        ) : ca.dataType === "BOOLEAN" ? (
                          <select
                            value={
                              nonVariantValues[ca.code] === true
                                ? "true"
                                : nonVariantValues[ca.code] === false
                                ? "false"
                                : ""
                            }
                            onChange={(e) => {
                              const val = e.target.value;
                              setNonVariantValues({
                                ...nonVariantValues,
                                [ca.code]: val === "true" ? true : val === "false" ? false : "",
                              });
                            }}
                            className="h-9 w-full border border-slate-200 rounded-md px-3 text-xs text-slate-700 bg-white focus:outline-none focus:border-slate-400"
                          >
                            <option value="">Chọn</option>
                            <option value="true">Có / Đúng</option>
                            <option value="false">Không / Sai</option>
                          </select>
                        ) : (
                          <Input
                            type="text"
                            placeholder={`Nhập ${ca.name.toLowerCase()}`}
                            value={nonVariantValues[ca.code] || ""}
                            onChange={(e) =>
                              setNonVariantValues({
                                ...nonVariantValues,
                                [ca.code]: e.target.value,
                              })
                            }
                            className="border-slate-200 h-9 text-xs focus-visible:ring-shop_dark_green/10"
                          />
                        )}
                      </div>
                    ))}
                </div>
              )}
            </div>

            {/* If no variant defining attributes, show a quick button to go to SKU matrix */}
            {!categoryAttributes.some((ca) => ca.isVariantDefining) && (
              <div className="flex justify-end pt-4 border-t border-slate-100">
                <Button
                  type="button"
                  onClick={() => scrollToSection(3)}
                  className="bg-shop_dark_green hover:bg-shop_dark_green/90 text-white cursor-pointer px-5 h-10 text-xs font-bold gap-1.5 shadow-sm"
                >
                  Tiếp tục đến SKU Matrix <ArrowRight className="size-4" />
                </Button>
              </div>
            )}

            {/* Area B: Variant attributes (isVariantDefining: true) */}
            {categoryAttributes.some((ca) => ca.isVariantDefining) && (
              <div className="space-y-4 pt-4 border-t border-slate-100">
                <h3 className="text-xs font-bold text-slate-800 flex items-center gap-1.5 pb-2 border-b border-slate-100">
                  <Settings2 className="size-4 text-shop_dark_green" /> Khu vực B: Thuộc tính biến thể (Variant defining Attributes)
                </h3>
                <div className="space-y-6">
                  <p className="text-[11px] text-slate-500">
                    Nhập hoặc chọn các thuộc tính để tiến hành sinh ma trận tổ hợp biến thể Cartesian.
                  </p>
                  {categoryAttributes
                    .filter((ca) => ca.isVariantDefining)
                    .map((ca) => (
                      <div key={ca.id} className="space-y-2.5 bg-slate-50/50 border border-slate-100 p-4 rounded-xl">
                        <Label className="text-slate-700 font-bold text-xs block">
                          {ca.name} {ca.unit ? `(${ca.unit})` : ""}
                        </Label>

                        {/* Selectable preconfigured enum list */}
                        {ca.enumValues && ca.enumValues.length > 0 && (
                          <div className="space-y-1.5">
                            <span className="text-[10px] text-slate-400 font-semibold block">Giá trị cấu hình sẵn:</span>
                            <div className="flex flex-wrap gap-1.5">
                              {(ca.enumValues || []).map((val: any) => {
                                const strVal = typeof val === "string" ? val : val.value;
                                const isChecked = (selectedVariants[ca.code] || []).includes(strVal);
                                return (
                                  <button
                                    key={strVal}
                                    type="button"
                                    onClick={() => {
                                      const currentList = selectedVariants[ca.code] || [];
                                      const newList = isChecked
                                        ? currentList.filter((v) => v !== strVal)
                                        : [...currentList, strVal];
                                      setSelectedVariants({
                                        ...selectedVariants,
                                        [ca.code]: newList,
                                      });
                                    }}
                                    className={`px-3 py-1 rounded-full text-2xs font-semibold border transition-all cursor-pointer ${
                                      isChecked
                                        ? "bg-shop_dark_green text-white border-shop_dark_green shadow-2xs"
                                        : "bg-white text-slate-600 border-slate-200 hover:bg-slate-50"
                                    }`}
                                  >
                                    {strVal}
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        )}

                        {/* Custom tag inputs */}
                        <div className="space-y-2 pt-1">
                          <span className="text-[10px] text-slate-400 font-semibold block">Giá trị tự nhập hoặc đã chọn:</span>
                          <div className="flex flex-wrap items-center gap-1.5">
                            {(selectedVariants[ca.code] || []).map((val) => (
                              <Badge key={val} className="bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200 gap-1 px-2.5 py-1 text-2xs">
                                {val}
                                <button
                                  type="button"
                                  onClick={() => {
                                    setSelectedVariants({
                                      ...selectedVariants,
                                      [ca.code]: (selectedVariants[ca.code] || []).filter((v) => v !== val),
                                    });
                                  }}
                                  className="text-slate-400 hover:text-slate-650 cursor-pointer font-bold"
                                  >
                                  ×
                                </button>
                              </Badge>
                            ))}
                            <div className="flex gap-1.5 max-w-[200px] items-center">
                              <Input
                                type="text"
                                placeholder="Thêm giá trị..."
                                value={newCustomTag[ca.code] || ""}
                                onChange={(e) =>
                                  setNewCustomTag({ ...newCustomTag, [ca.code]: e.target.value })
                                }
                                onKeyDown={(e) => {
                                  if (e.key === "Enter") {
                                    e.preventDefault();
                                    handleAddCustomTag(ca.code);
                                  }
                                }}
                                className="border-slate-200 h-8 text-2xs focus-visible:ring-shop_dark_green/10"
                              />
                              <Button
                                type="button"
                                size="sm"
                                className="h-8 bg-slate-900 hover:bg-slate-800 text-white text-[10px] cursor-pointer"
                                onClick={() => handleAddCustomTag(ca.code)}
                              >
                                Thêm
                              </Button>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}

                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
