import React from 'react'
import { redirect } from 'next/navigation'
import { getAdminSession } from '@/features/auth/actions'
import Container from '@/components/Container'
import Logo from '@/components/Logo'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { AdminLoginForm } from '../components/AdminLoginForm'

export default async function AdminLoginPage() {
  const user = await getAdminSession()

  // If already logged in as Admin, redirect directly to the admin dashboard
  if (user) {
    redirect('/admin')
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-100 py-12 px-4 sm:px-6 lg:px-8">
      <Container className="max-w-md w-full">
        <Card className="border-none shadow-2xl bg-white overflow-hidden rounded-2xl animate-in fade-in zoom-in duration-300">
          <CardHeader className="text-center pb-4 pt-8">
            <div className="flex justify-center mb-3">
              <Logo />
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight text-slate-800 font-display">
              Hệ thống Quản trị
            </CardTitle>
            <CardDescription className="text-sm text-slate-400 mt-1">
              Vui lòng đăng nhập bằng tài khoản Admin.
            </CardDescription>
          </CardHeader>

          <CardContent className="p-6 pt-4">
            <AdminLoginForm />
          </CardContent>
        </Card>
      </Container>
    </div>
  )
}
