"use client";

import React from "react";
import Link from "next/link";
import { ShoppingBag, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export function EmptyCart() {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-slate-100 bg-white p-8 text-center shadow-xs md:p-12">
      <div className="flex h-20 w-20 items-center justify-center rounded-full bg-emerald-50 text-shop_light_green md:h-24 md:w-24">
        <ShoppingBag className="h-10 w-10 md:h-12 md:w-12" />
      </div>
      <h2 className="mt-6 text-xl font-bold text-slate-800 md:text-2xl">
        Giỏ hàng của bạn đang trống
      </h2>
      <p className="mt-2 max-w-md text-sm text-slate-500 md:text-base">
        Có vẻ như bạn chưa chọn sản phẩm nào. Hãy khám phá hàng ngàn sản phẩm công nghệ chất lượng tại ABTechZone!
      </p>
      <Link href="/">
        <Button
          className="mt-6 bg-shop_light_green hover:bg-shop_dark_green text-white font-semibold px-6 py-2.5 rounded-xl shadow-sm transition-all cursor-pointer"
        >
          Khám phá sản phẩm ngay <ArrowRight className="ml-2 h-4 w-4" />
        </Button>
      </Link>
    </div>
  );
}
