import React from 'react'
import { DollarSign, ShoppingBag, Users, BarChart3 } from 'lucide-react'
import { Card, CardContent, CardHeader } from '@/components/ui/card'

const stats = [
  {
    title: 'Doanh thu tháng',
    value: '124,500,000 ₫',
    desc: '+12.5% so với tháng trước',
    icon: DollarSign,
    color: 'text-emerald-600 bg-emerald-50',
  },
  {
    title: 'Đơn hàng mới',
    value: '145 đơn hàng',
    desc: '+8.2% so với tuần trước',
    icon: ShoppingBag,
    color: 'text-shop_orange bg-orange-50',
  },
  {
    title: 'Khách hàng mới',
    value: '48 thành viên',
    desc: '+23% so với tháng trước',
    icon: Users,
    color: 'text-blue-600 bg-blue-50',
  },
  {
    title: 'Tỷ lệ chuyển đổi',
    value: '3.42%',
    desc: '+1.1% so với tháng trước',
    icon: BarChart3,
    color: 'text-indigo-600 bg-indigo-50',
  },
]

export default function StatsGrid() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
      {stats.map((stat, i) => {
        const Icon = stat.icon
        return (
          <Card key={i} className="border-none shadow-md bg-white hover:shadow-lg transition-all duration-300">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <span className="text-sm font-semibold text-slate-500">{stat.title}</span>
              <div className={`p-2 rounded-lg ${stat.color}`}>
                <Icon className="size-5" />
              </div>
            </CardHeader>
            <CardContent className="pt-2">
              <div className="text-2xl font-bold text-slate-800 tracking-tight">{stat.value}</div>
              <p className="text-xs text-slate-400 mt-1.5 font-medium flex items-center gap-1">
                <span>{stat.desc}</span>
              </p>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}
