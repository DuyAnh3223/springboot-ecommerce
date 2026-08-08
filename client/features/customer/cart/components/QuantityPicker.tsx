"use client";

import React from "react";
import { Minus, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";

interface QuantityPickerProps {
  quantity: number;
  onUpdate: (newQty: number) => void;
  disabled?: boolean;
  min?: number;
  productName?: string;
}

export function QuantityPicker({
  quantity,
  onUpdate,
  disabled = false,
  min = 1,
  productName,
}: QuantityPickerProps) {
  const isMinusDisabled = disabled || quantity <= min;

  return (
    <div className="flex items-center space-x-1">
      <Button
        type="button"
        variant="outline"
        size="icon"
        disabled={isMinusDisabled}
        onClick={() => onUpdate(quantity - 1)}
        className="h-8 w-8 rounded-lg border-slate-200 text-slate-600 hover:border-slate-300 hover:bg-slate-100 disabled:opacity-40"
        aria-label={productName ? `Giảm số lượng cho ${productName}` : "Giảm số lượng"}
      >
        <Minus className="h-3.5 w-3.5" />
      </Button>
      <span className="w-10 text-center text-sm font-semibold text-slate-800">
        {quantity}
      </span>
      <Button
        type="button"
        variant="outline"
        size="icon"
        disabled={disabled}
        onClick={() => onUpdate(quantity + 1)}
        className="h-8 w-8 rounded-lg border-slate-200 text-slate-600 hover:border-slate-300 hover:bg-slate-100 disabled:opacity-40"
        aria-label={productName ? `Tăng số lượng cho ${productName}` : "Tăng số lượng"}
      >
        <Plus className="h-3.5 w-3.5" />
      </Button>
    </div>
  );
}
