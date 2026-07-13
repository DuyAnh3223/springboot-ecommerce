import React from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

export default function QuickLinks() {
  return (
    <Card className="border-none shadow-md bg-white p-6">
      <CardHeader className="px-0 pt-0 pb-4 border-b border-slate-100">
        <CardTitle className="text-base font-bold text-slate-800">Lối tắt thao tác nhanh</CardTitle>
        <CardDescription className="text-xs text-slate-400 mt-0.5">Liên kết đến các mục thiết lập chính.</CardDescription>
      </CardHeader>
      <CardContent className="px-0 pt-4 space-y-3">
        {[
          { label: 'Thêm sản phẩm mới', desc: 'Tạo sản phẩm, thuộc tính mới', path: '/admin/products' },
          { label: 'Xử lý đơn hàng đợi duyệt', desc: 'Giao nhận, hóa đơn đơn hàng', path: '/admin/orders' },
          { label: 'Cấu hình cổng thanh toán', desc: 'Cài đặt thẻ, ví điện tử', path: '/admin/settings' },
        ].map((link, i) => (
          <a
            key={i}
            href={link.path}
            className="block p-3 rounded-xl border border-slate-100 hover:border-shop_light_green/30 hover:bg-emerald-50/10 hover:shadow-sm transition-all group"
          >
            <p className="text-sm font-semibold text-slate-700 group-hover:text-shop_light_green transition-colors">{link.label}</p>
            <p className="text-xs text-slate-400 mt-0.5">{link.desc}</p>
          </a>
        ))}
      </CardContent>
    </Card>
  )
}
