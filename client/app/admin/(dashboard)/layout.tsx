import React from 'react'
import { redirect } from 'next/navigation'
import { getAdminSession } from '@/features/auth/actions'
import SideMenu from '../components/SideMenu'
import Header from '../components/Header'

export default async function AdminDashboardLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const user = await getAdminSession()

  if (!user) {
    redirect('/admin/login')
  }

  return (
    <div className="flex bg-slate-50 min-h-screen">
      {/* Sidebar */}
      <SideMenu />

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        <Header />
        <main className="flex-1 p-2 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  )
}

