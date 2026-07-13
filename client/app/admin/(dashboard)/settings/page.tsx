import React from 'react'
import { Settings, Save, Shield, Sliders } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Input } from '@/components/ui/input'

export default function SettingsPage() {
  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-800 tracking-tight font-display">Cài đặt hệ thống</h2>
          <p className="text-sm text-slate-500 mt-1">Cấu hình các tham số hoạt động chung của cửa hàng.</p>
        </div>
        <Button className="bg-shop_light_green hover:bg-shop_light_green/90 text-white font-semibold cursor-pointer gap-1.5 h-10 self-start sm:self-auto">
          <Save className="size-4.5" /> Lưu cài đặt
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2 space-y-6">
          {/* General Config */}
          <Card className="border-none shadow-md bg-white p-6 space-y-4">
            <CardHeader className="p-0 pb-3 border-b border-slate-100 flex flex-row items-center gap-2">
              <Sliders className="size-5 text-slate-500" />
              <div>
                <CardTitle className="text-base font-bold text-slate-800">Cấu hình cửa hàng</CardTitle>
                <CardDescription className="text-xs text-slate-400 mt-0.5">Tên hiển thị và các thông tin cơ bản.</CardDescription>
              </div>
            </CardHeader>
            <CardContent className="p-0 pt-3 space-y-4">
              <div className="space-y-1.5">
                <Label htmlFor="shop-name">Tên cửa hàng</Label>
                <Input id="shop-name" defaultValue="ABTechZone" className="border-slate-200 focus-visible:ring-shop_light_green/20 focus-visible:border-shop_light_green" />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="shop-email">Email liên hệ</Label>
                <Input id="shop-email" type="email" defaultValue="support@abtechzone.com" className="border-slate-200 focus-visible:ring-shop_light_green/20 focus-visible:border-shop_light_green" />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Info panel */}
        <div className="space-y-6">
          <Card className="border-none shadow-md bg-white p-6 space-y-4">
            <CardHeader className="p-0 pb-3 border-b border-slate-100 flex flex-row items-center gap-2">
              <Shield className="size-5 text-slate-500" />
              <div>
                <CardTitle className="text-base font-bold text-slate-800">Bảo mật hệ thống</CardTitle>
                <CardDescription className="text-xs text-slate-400 mt-0.5">Trạng thái cấu hình cổng bảo mật.</CardDescription>
              </div>
            </CardHeader>
            <CardContent className="p-0 pt-3 text-slate-600 text-sm">
              <p>Phiên bản API hiện tại: <strong>v1.0.0</strong></p>
              <p className="mt-2">Trạng thái chứng chỉ SSL: <span className="text-emerald-600 font-semibold">Hợp lệ</span></p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
