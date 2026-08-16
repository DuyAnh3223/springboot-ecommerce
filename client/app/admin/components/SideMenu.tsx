'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/shared/utils/cn'
import { 
  LayoutDashboard, 
  Package, 
  ShoppingCart, 
  LayoutGrid,
  Users, 
  Settings, 
  ArrowLeft,
  ChevronDown,
  ChevronRight,
  Ticket,
  LucideIcon
} from 'lucide-react'
import Logo from '@/components/Logo'
import { useState } from 'react'

interface SubMenuItem {
  title: string;
  href: string;
}

interface MenuItem {
  title: string;
  href?: string;
  icon: LucideIcon;
  subMenu?: SubMenuItem[];
}

const menuItems: MenuItem[] = [
  {
    title: 'Tổng quan',
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
    title: 'Danh mục',
    icon: LayoutGrid,
    subMenu: [
      {
        title: 'Danh sách danh mục',
        href: '/admin/categories',
      },
      {
        title: 'Thuộc tính',
        href: '/admin/attributes',
      },
    ]
  },
  {
    title: 'Sản phẩm',
    href: '/admin/products',
    icon: Package,
  },
  {
    title: 'Mã giảm giá',
    href: '/admin/vouchers',
    icon: Ticket,
  },
  {
    title: 'Cài đặt',
    href: '/admin/settings',
    icon: Settings,
  },
]

export default function SideMenu() {
  const pathname = usePathname()
  const [userToggledSubMenus, setUserToggledSubMenus] = useState<Record<string, boolean>>({})

  const toggleSubMenu = (title: string, currentIsOpen: boolean) => {
    setUserToggledSubMenus((prev) => ({
      ...prev,
      [title]: !currentIsOpen,
    }))
  }

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
          const hasSubMenu = !!item.subMenu
          const isSubActive = hasSubMenu
            ? item.subMenu?.some(sub => pathname === sub.href) ?? false
            : pathname === item.href

          const isSubMenuOpen = userToggledSubMenus[item.title] !== undefined
            ? userToggledSubMenus[item.title]
            : isSubActive

          return (
            <div key={item.title} className="space-y-1">
              {hasSubMenu ? (
                <button
                  type="button"
                  onClick={() => toggleSubMenu(item.title, isSubMenuOpen)}
                  className={cn(
                    "w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer group text-left",
                    isSubActive
                      ? "bg-slate-800 text-white"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  )}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={cn(
                      "size-5 transition-transform group-hover:scale-105",
                      isSubActive ? "text-white" : "text-slate-400 group-hover:text-white"
                    )} />
                    <span>{item.title}</span>
                  </div>
                  {isSubMenuOpen ? (
                    <ChevronDown className="size-4 text-slate-400 group-hover:text-white" />
                  ) : (
                    <ChevronRight className="size-4 text-slate-400 group-hover:text-white" />
                  )}
                </button>
              ) : (
                <Link
                  href={item.href || '#'}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer group",
                    isSubActive
                      ? "bg-shop_light_green text-white shadow-md shadow-shop_light_green/20"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  )}
                >
                  <Icon className={cn(
                    "size-5 transition-transform group-hover:scale-105",
                    isSubActive ? "text-white" : "text-slate-400 group-hover:text-white"
                  )} />
                  <span>{item.title}</span>
                </Link>
              )}

              {/* Submenu list */}
              {hasSubMenu && isSubMenuOpen && (
                <div className="pl-4 pr-1 py-1 space-y-1 border-l border-slate-800 ml-5 animate-in slide-in-from-top-1 duration-200">
                  {item.subMenu?.map((sub) => {
                    const isChildActive = pathname === sub.href
                    return (
                      <Link
                        key={sub.href}
                        href={sub.href}
                        className={cn(
                          "block px-3 py-1.5 rounded-md text-xs font-semibold transition-all duration-200 cursor-pointer",
                          isChildActive
                            ? "bg-shop_light_green text-white shadow-sm shadow-shop_light_green/10"
                            : "text-slate-400 hover:bg-slate-800 hover:text-white"
                        )}
                      >
                        {sub.title}
                      </Link>
                    )
                  })}
                </div>
              )}
            </div>
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
