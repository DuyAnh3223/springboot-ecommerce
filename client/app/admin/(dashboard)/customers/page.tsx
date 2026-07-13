import React from 'react'
import { Users, Search, Filter, Plus } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

export default function CustomersPage() {
  return (
    <div className="space-y-2 animate-in fade-in duration-300">
      
      {/* Filter and Search */}
      <Card className="border-none shadow-md bg-white p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="relative flex-1">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              <Search className="size-4.5" />
            </span>
            <Input 
              type="text" 
              placeholder="Tìm kiếm theo tên, email, số điện thoại..." 
              className="pl-9 h-10 border-slate-200 focus-visible:ring-shop_light_green/20 focus-visible:border-shop_light_green" 
            />
          </div>
          <div className="flex gap-3 shrink-0">
            <Button variant="outline" className="border-slate-200 text-slate-600 hover:text-slate-800 gap-1.5 h-10 cursor-pointer">
              <Filter className="size-4" /> Bộ lọc
            </Button>
          </div>
        </div>
      </Card>

      {/* Table Placeholder */}
      <Card className="border-none shadow-md bg-white p-6">
        <CardContent className="px-0 pt-0 pb-0 flex flex-col items-center justify-center py-16 text-center">
          <div className="p-4 bg-slate-50 rounded-full text-slate-400 mb-4 border border-slate-100">
            <Users className="size-12" />
          </div>
          <h3 className="text-lg font-bold text-slate-700">Chưa có dữ liệu khách hàng hiển thị</h3>
          <p className="text-sm text-slate-400 mt-1 max-w-sm">
            Hiện tại không có thông tin khách hàng nào được tìm thấy hoặc kết nối tới cơ sở dữ liệu.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
