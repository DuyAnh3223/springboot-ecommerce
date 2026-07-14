import React from 'react'
import { Package, Search, Filter, Plus } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'

export default function ProductsPage() {
  return (
    <div className="space-y-2 animate-in fade-in duration-300">
      

      <Card className="border-none shadow-md bg-white p-4">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="relative flex-1">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              <Search className="size-4.5" />
            </span>
            <Input 
              type="text" 
              placeholder="Tìm sản phẩm theo tên, SKU, mã vạch..." 
              className="pl-9 h-10 border-slate-200 focus-visible:ring-shop_light_green/20 focus-visible:border-shop_light_green" 
            />
          </div>
          <div className="flex gap-3 shrink-0">
            <Button variant="outline" className="border-slate-200 text-slate-600 hover:text-slate-800 gap-1.5 h-10 cursor-pointer">
              <Filter className="size-4" /> Danh mục
            </Button>
          </div>
        </div>
      </Card>

      <Card className="border-none shadow-md bg-white p-6">
        <CardContent className="px-0 pt-0 pb-0 flex flex-col items-center justify-center py-16 text-center">
          <div className="p-4 bg-slate-50 rounded-full text-slate-400 mb-4 border border-slate-100">
            <Package className="size-12" />
          </div>
          <h3 className="text-lg font-bold text-slate-700">Chưa có sản phẩm nào</h3>
          <p className="text-sm text-slate-400 mt-1 max-w-sm">
            Danh sách sản phẩm trong kho đang trống. Nhấn nút "Thêm sản phẩm" ở trên để bắt đầu thêm mới.
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
