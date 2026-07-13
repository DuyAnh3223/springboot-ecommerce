import React from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AlertTriangle } from 'lucide-react'

export default function RecentActivity() {
  return (
    <Card className="border-none shadow-md bg-white p-6">
      <CardHeader className="px-0 pt-0 pb-4 border-b border-slate-100 flex flex-row items-center justify-between">
        <div>
          <CardTitle className="text-base font-bold text-slate-800">Hoạt động gần đây</CardTitle>
          <CardDescription className="text-xs text-slate-400 mt-0.5">Danh sách các cập nhật trạng thái đơn hàng mới nhất.</CardDescription>
        </div>
        <button className="text-xs font-semibold text-shop_light_green hover:underline cursor-pointer">
          Xem tất cả
        </button>
      </CardHeader>
      <CardContent className="px-0 pt-4 space-y-4">
        {[
          { time: '10 phút trước', desc: 'Khách hàng Trần Văn A đặt đơn hàng #DH-9082', type: 'info' },
          { time: '30 phút trước', desc: 'Đơn hàng #DH-9071 đã được chuyển kho thành công', type: 'success' },
          { time: '1 giờ trước', desc: 'Đã hoàn tiền đơn hàng bị hủy #DH-9052', type: 'warning' },
          { time: '2 giờ trước', desc: 'Sản phẩm "MacBook Pro M3" đã sắp hết hàng trong kho', type: 'alert' },
        ].map((act, i) => (
          <div key={i} className="flex items-start justify-between gap-4 text-sm border-b border-slate-50 pb-3 last:border-0 last:pb-0">
            <div>
              <p className="text-slate-700 font-medium">{act.desc}</p>
              <p className="text-xs text-slate-400 mt-0.5">{act.time}</p>
            </div>
            {act.type === 'alert' && (
              <span className="flex items-center gap-1 text-xs font-semibold text-rose-600 bg-rose-50 px-2 py-0.5 rounded-full border border-rose-100 shrink-0">
                <AlertTriangle className="size-3" /> Cảnh báo
              </span>
            )}
          </div>
        ))}
      </CardContent>
    </Card>
  )
}
