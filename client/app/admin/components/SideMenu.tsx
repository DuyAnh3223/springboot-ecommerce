'use client'

import React from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import { 
  LayoutDashboard, 
  Package, 
  ShoppingCart, 
  Users, 
  Settings, 
  ArrowLeft 
} from 'lucide-react'
import Logo from '@/components/Logo'

const menuItems = [
  {
    title: 'Dashboard',
    href: '/admin',
    icon: LayoutDashboard,
  },
  {
    title: 'Khách hàng',
    href: '/admin/customers',
    icon: Users,
  },
  {
    title: 'Đơn hàng',
    href: '/admin/orders',
    icon: ShoppingCart,
  },
  {
    title: 'Sản phẩm',
    href: '/admin/products',
    icon: Package,
  },
  {
    title: 'Cài đặt',
    href: '/admin/settings',
    icon: Settings,
  },
]

export default function SideMenu() {
  const pathname = usePathname()

  return (
    <aside className="w-48 bg-slate-900 text-white min-h-screen flex flex-col border-r border-slate-800 sticky top-0">
      {/* Sidebar Header */}
      <div className="p-3 border-b border-slate-800 flex items-center justify-between">
        <Logo className="text-white" spanDesign="text-shop_light_green" />
      </div>

      {/* Nav Menu */}
      <nav className="flex-1 px-4 py-6 space-y-1.5">
        <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider px-3 mb-3">
          Quản lý hệ thống
        </div>
        {menuItems.map((item) => {
          const Icon = item.icon
          const isActive = pathname === item.href

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer group",
                isActive
                  ? "bg-shop_light_green text-white shadow-md shadow-shop_light_green/20"
                  : "text-slate-300 hover:bg-slate-800 hover:text-white"
              )}
            >
              <Icon className={cn(
                "size-5 transition-transform group-hover:scale-105",
                isActive ? "text-white" : "text-slate-400 group-hover:text-white"
              )} />
              <span>{item.title}</span>
            </Link>
          )
        })}
      </nav>

      {/* Sidebar Footer */}
      <div className="p-4 border-t border-slate-800">
        <Link
          href="/"
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-slate-300 hover:bg-slate-800 hover:text-white transition-all cursor-pointer"
        >
          <ArrowLeft className="size-4" />
          <span>Về trang cửa hàng</span>
        </Link>
      </div>
    </aside>
  )
}
