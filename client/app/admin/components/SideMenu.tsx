'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { cn } from '@/lib/utils'
import { 
  LayoutDashboard, 
  Package, 
  ShoppingCart, 
  LayoutGrid,
  Users, 
  Settings, 
  ArrowLeft,
  ChevronDown,
  ChevronRight
} from 'lucide-react'
import Logo from '@/components/Logo'
import { useState, useEffect } from 'react'

interface SubMenuItem {
  title: string;
  href: string;
}

interface MenuItem {
  title: string;
  href?: string;
  icon: any;
  subMenu?: SubMenuItem[];
}

const menuItems: MenuItem[] = [
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
    title: 'Danh mục',
    icon: LayoutGrid,
    subMenu: [
      {
        title: 'Danh sách danh mục',
        href: '/admin/categories',
      },
      {
        title: 'Thuộc tính ',
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
    title: 'Cài đặt',
    href: '/admin/settings',
    icon: Settings,
  },
]

export default function SideMenu() {
  const pathname = usePathname()
  const [openSubMenus, setOpenSubMenus] = useState<Record<string, boolean>>({})

  useEffect(() => {
    // Open submenu automatically if active path matches any sub-item
    const initialOpenState: Record<string, boolean> = {}
    menuItems.forEach(item => {
      if (item.subMenu) {
        const hasActiveSub = item.subMenu.some(sub => pathname === sub.href)
        if (hasActiveSub) {
          initialOpenState[item.title] = true
        }
      }
    })
    setOpenSubMenus(initialOpenState)
  }, [pathname])

  const toggleSubMenu = (title: string) => {
    setOpenSubMenus(prev => ({
      ...prev,
      [title]: !prev[title]
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
          const isSubMenuOpen = !!openSubMenus[item.title]
          const isActive = hasSubMenu
            ? item.subMenu?.some(sub => pathname === sub.href)
            : pathname === item.href

          return (
            <div key={item.title} className="space-y-1">
              {hasSubMenu ? (
                <button
                  type="button"
                  onClick={() => toggleSubMenu(item.title)}
                  className={cn(
                    "w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200 cursor-pointer group text-left",
                    isActive
                      ? "bg-slate-800 text-white"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  )}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={cn(
                      "size-5 transition-transform group-hover:scale-105",
                      isActive ? "text-white" : "text-slate-400 group-hover:text-white"
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
              )}

              {/* Submenu list */}
              {hasSubMenu && isSubMenuOpen && (
                <div className="pl-4 pr-1 py-1 space-y-1 border-l border-slate-800 ml-5 animate-in slide-in-from-top-1 duration-200">
                  {item.subMenu?.map((sub) => {
                    const isSubActive = pathname === sub.href
                    return (
                      <Link
                        key={sub.href}
                        href={sub.href}
                        className={cn(
                          "block px-3 py-1.5 rounded-md text-xs font-semibold transition-all duration-200 cursor-pointer",
                          isSubActive
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
