'use client'

import React, { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/features/auth/stores/auth.store'
import { signoutAction } from '@/features/auth/actions'
import { Button } from '@/components/ui/button'
import { LogOut, User, Bell, Loader2 } from 'lucide-react'

export default function Header() {
  const { user, clear } = useAuthStore()
  const router = useRouter()
  const [loading, setLoading] = useState(false)

  const handleLogout = async () => {
    setLoading(true)
    try {
      await signoutAction()
      clear()
      router.push('/admin/login')
      router.refresh()
    } catch (err) {
      console.error('Logout error:', err)
    } finally {
      setLoading(false)
    }
  }

  const displayName = user?.firstName 
    ? `${user.lastName || ''} ${user.firstName}`.trim() 
    : user?.username || 'Admin'

  return (
    <header className="h-16 border-b border-slate-200 bg-white/80 backdrop-blur-md sticky top-0 z-40 px-8 flex items-center justify-between">
      {/* Title */}
      <div>
        <h1 className="text-lg font-bold text-slate-800 font-display">
          Hệ thống Quản trị ABTechZone
        </h1>
      </div>

      {/* Right Controls */}
      <div className="flex items-center gap-6">
        {/* Notification Bell */}
        <button 
          type="button" 
          className="text-slate-500 hover:text-slate-800 transition-colors relative cursor-pointer"
        >
          <Bell className="size-5" />
          <span className="absolute top-0 right-0 size-2 bg-shop_orange rounded-full" />
        </button>

        {/* User Info */}
        <div className="flex items-center gap-3">
          <div className="size-8 bg-slate-100 rounded-full border border-slate-200 flex items-center justify-center text-slate-600">
            <User className="size-4.5" />
          </div>
          <div className="text-left hidden md:block">
            <p className="text-xs text-slate-400 font-medium">Xin chào,</p>
            <p className="text-sm font-semibold text-slate-700 leading-tight">{displayName}</p>
          </div>
        </div>

        {/* Logout Button */}
        <Button
          variant="outline"
          size="sm"
          disabled={loading}
          onClick={handleLogout}
          className="text-destructive hover:bg-destructive/10 border-destructive/20 hover:text-destructive gap-1.5 h-8.5 text-xs font-semibold cursor-pointer"
        >
          {loading ? (
            <Loader2 className="size-3.5 animate-spin" />
          ) : (
            <LogOut className="size-3.5" />
          )}
          Đăng xuất
        </Button>
      </div>
    </header>
  )
}
