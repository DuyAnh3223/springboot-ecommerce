import React from "react";
import { TicketPercent, Sparkles, ShieldCheck, Zap } from "lucide-react";

export function CustomerVoucherHero() {
  return (
    <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-slate-800 to-shop_dark_green text-white p-6 sm:p-10 mb-8 shadow-xl">
      {/* Background glowing shapes */}
      <div className="absolute -top-24 -right-24 w-80 h-80 rounded-full bg-shop_light_green/20 blur-3xl pointer-events-none" />
      <div className="absolute -bottom-24 -left-24 w-80 h-80 rounded-full bg-emerald-500/10 blur-3xl pointer-events-none" />

      <div className="relative z-10 max-w-3xl">
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/10 backdrop-blur-md text-emerald-300 text-xs font-bold tracking-wide uppercase mb-4 border border-white/10">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Kho Ưu Đãi Độc Quyền ABTechZone</span>
        </div>

        <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-tight mb-3">
          Săn Mã Giảm Giá, <br className="hidden sm:inline" />
          <span className="bg-gradient-to-r from-emerald-400 via-teal-300 to-shop_light_green bg-clip-text text-transparent">
            Thỏa Sức Nâng Cấp PC
          </span>
        </h1>

        <p className="text-slate-300 text-sm sm:text-base leading-relaxed mb-6 max-w-2xl">
          Khám phá và lưu ngay các mã voucher giảm giá cực hời cho linh kiện máy tính, VGA, CPU, RAM và phụ kiện chính hãng. Áp dụng dễ dàng ngay khi đặt hàng!
        </p>

        {/* Feature Highlights */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-4 border-t border-white/10 text-xs text-slate-300">
          <div className="flex items-center gap-2">
            <TicketPercent className="w-4 h-4 text-emerald-400 flex-shrink-0" />
            <span>Mã giảm giá cập nhật liên tục</span>
          </div>
          <div className="flex items-center gap-2">
            <Zap className="w-4 h-4 text-emerald-400 flex-shrink-0" />
            <span>Áp dụng tự động nhanh chóng</span>
          </div>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0" />
            <span>Cam kết 100% chính hãng</span>
          </div>
        </div>
      </div>
    </div>
  );
}
