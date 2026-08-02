"use client";

import React from "react";
import { CategoryAttributeResponse } from "@/features/attributes/attribute.type";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Layers, Sliders, Loader2 } from "lucide-react";

interface StepAttributesFormProps {
  selectedCategoryName: string;
  categoryAttributes: CategoryAttributeResponse[];
  loadingAttributes: boolean;
  nonVariantValues: Record<string, any>;
  setNonVariantValues: (val: Record<string, any>) => void;
}

export function StepAttributesForm({
  selectedCategoryName,
  categoryAttributes,
  loadingAttributes,
  nonVariantValues,
  setNonVariantValues,
}: StepAttributesFormProps) {
  const commonAttributes = categoryAttributes.filter((ca) => !ca.isVariantDefining);

  return (
    <Card className="border-none shadow-sm bg-white overflow-hidden">
      <CardHeader className="bg-slate-50/50 border-b border-slate-100 pr-6">
        <div>
          <CardTitle className="text-base font-bold text-slate-800 flex items-center gap-2">
            <Sliders className="size-4.5 text-shop_dark_green" /> Bước 2: Nhập Thuộc Tính Chung (EAV Attributes)
          </CardTitle>
          <CardDescription className="text-xs text-slate-450">
            Thiết lập giá trị cho các thuộc tính thông số kỹ thuật chung tương ứng với danh mục{" "}
            <strong className="text-slate-700 font-bold">{selectedCategoryName}</strong>.
          </CardDescription>
        </div>
      </CardHeader>
      <CardContent className="p-6 space-y-6">
        {loadingAttributes ? (
          <div className="flex flex-col items-center justify-center py-12 text-slate-400">
            <Loader2 className="size-8 animate-spin mb-2 text-shop_dark_green" />
            <p className="text-xs">Đang tải danh sách thuộc tính cho danh mục...</p>
          </div>
        ) : commonAttributes.length === 0 ? (
          <div className="text-center py-8 text-slate-450 bg-slate-50 border border-slate-150 rounded-xl flex flex-col items-center justify-center">
            <Layers className="size-8 text-slate-300 mb-2" />
            <p className="text-xs font-semibold">Danh mục này không có thuộc tính chung nào cần nhập.</p>
          </div>
        ) : (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {commonAttributes.map((ca) => (
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
          </div>
        )}
      </CardContent>
    </Card>
  );
}
