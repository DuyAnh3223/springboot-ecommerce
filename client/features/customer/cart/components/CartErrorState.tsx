"use client";

import React from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

interface CartErrorStateProps {
  message?: string;
  onRetry?: () => void;
}

export function CartErrorState({
  message = "Không thể tải thông tin giỏ hàng. Vui lòng thử lại sau.",
  onRetry,
}: CartErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-red-100 bg-red-50/50 p-8 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-red-100 text-red-600">
        <AlertCircle className="h-7 w-7" />
      </div>
      <h3 className="mt-4 text-lg font-bold text-slate-800">
        Đã có lỗi xảy ra
      </h3>
      <p className="mt-1 text-sm text-slate-600">{message}</p>
      {onRetry && (
        <Button
          onClick={onRetry}
          variant="outline"
          className="mt-5 border-red-200 text-red-700 hover:bg-red-100 font-semibold"
        >
          <RefreshCw className="mr-2 h-4 w-4" /> Thử lại
        </Button>
      )}
    </div>
  );
}
