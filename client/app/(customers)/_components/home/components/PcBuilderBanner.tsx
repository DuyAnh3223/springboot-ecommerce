import Link from "next/link";
import { Wrench, CheckCircle, Cpu, Zap, ShieldCheck } from "lucide-react";

export default function PcBuilderBanner() {
  return (
    <section id="pc-builder" className="py-12 bg-slate-900 border-b border-slate-800 scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4">
        <div className="relative rounded-3xl overflow-hidden bg-gradient-to-r from-slate-950 via-slate-900 to-indigo-950 border border-slate-800 p-8 sm:p-12 shadow-2xl">
          {/* Background Ambient Lights */}
          <div className="absolute top-0 right-0 w-96 h-96 bg-rose-600/10 rounded-full blur-3xl pointer-events-none" />
          <div className="absolute bottom-0 left-0 w-96 h-96 bg-indigo-600/10 rounded-full blur-3xl pointer-events-none" />

          <div className="relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            {/* Content Left */}
            <div className="lg:col-span-8 space-y-4">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-600/20 border border-indigo-500/30 text-indigo-400 text-xs font-bold uppercase tracking-wider">
                <Wrench className="w-3.5 h-3.5" />
                CÔNG CỤ ĐỘC QUYỀN ABTECHZONE
              </span>

              <h2 className="text-2xl sm:text-4xl font-extrabold text-white leading-tight">
                TỰ XÂY DỰNG CẤU HÌNH PC THEO Ý MUỐN
              </h2>

              <p className="text-slate-300 text-sm sm:text-base max-w-2xl leading-relaxed">
                Tự động kiểm tra độ tương thích giữa CPU, Mainboard, RAM, VGA & Nguồn PSU.
                Đội ngũ kỹ thuật viên ABTechZone hỗ trợ lắp đặt, đi dây thẩm mỹ & cài đặt phần mềm hoàn toàn miễn phí.
              </p>

              {/* Feature Checklist */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
                  <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Check tương thích 100%</span>
                </div>
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
                  <Cpu className="w-4 h-4 text-rose-400 shrink-0" />
                  <span>Tối ưu theo ngân sách</span>
                </div>
                <div className="flex items-center gap-2 text-xs font-semibold text-slate-200">
                  <ShieldCheck className="w-4 h-4 text-indigo-400 shrink-0" />
                  <span>Bảo hành tận nhà 1 đổi 1</span>
                </div>
              </div>
            </div>

            {/* Action Right */}
            <div className="lg:col-span-4 flex flex-col items-start lg:items-end justify-center">
              <Link
                href="/category"
                className="inline-flex items-center gap-2 px-8 py-4 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-extrabold text-sm shadow-xl shadow-rose-950/50 hover:shadow-rose-600/40 transition-all hover:scale-105"
              >
                <Zap className="w-4 h-4 fill-white" />
                <span>Khám Phá Linh Kiện Ngay</span>
              </Link>
              <span className="text-[11px] text-slate-400 mt-2 font-medium">
                Tư vấn miễn phí qua Hotline: 0958.648.597
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
