import React from 'react'
import { TrendingUp } from 'lucide-react'

export default function WelcomeBanner() {
  return (
    <div className="bg-gradient-to-r from-slate-900 to-slate-800 p-6 rounded-2xl text-white shadow-lg flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 className="text-xl font-bold font-display">Hệ thống Quản lý Bán hàng</h2>
        <p className="text-sm text-slate-300 mt-1">
          Chào mừng bạn đến với trang quản trị cửa hàng ABTechZone. Tại đây bạn có thể xem các báo cáo tổng quan về doanh thu và đơn hàng.
        </p>
      </div>
      <div className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-lg border border-white/15 text-sm shrink-0 self-start md:self-auto">
        <TrendingUp className="size-4 text-emerald-400 animate-pulse" />
        <span>Hệ thống hoạt động bình thường</span>
      </div>
    </div>
  )
}
