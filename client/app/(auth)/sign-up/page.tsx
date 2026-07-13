'use client'

import React from 'react'
import Container from '@/components/Container'
import Logo from '@/components/Logo'
import { SignupForm } from '@/features/auth/components/signup-form'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import Link from 'next/link'
import { useRouter } from 'next/navigation'

export default function SignUpPage() {
  const router = useRouter()

  const handleSignupSuccess = (username: string) => {
    router.push(`/sign-in?username=${encodeURIComponent(username)}`)
  }

  return (
    <div className="min-h-[85vh] flex items-center justify-center bg-shop_light_pink py-12 px-4 sm:px-6 lg:px-8">
      <Container className="max-w-md w-full">
        <Card className="border-none shadow-2xl bg-white overflow-hidden rounded-2xl animate-in fade-in zoom-in duration-300">
          <CardHeader className="text-center pb-4 pt-8">
            <div className="flex justify-center mb-3">
              <Logo />
            </div>
            <CardTitle className="text-2xl font-bold tracking-tight text-shop_dark_green font-display">
              Tạo tài khoản mới
            </CardTitle>
            <CardDescription className="text-sm text-lightColor mt-1">
              Đăng ký tài khoản để nhận nhiều ưu đãi hấp dẫn.
            </CardDescription>
          </CardHeader>

          {/* Custom Animated Tabs */}
          <div className="px-6 pb-2">
            <div className="flex border border-gray-100 p-1 bg-gray-50/50 rounded-lg">
              <Link
                href="/sign-in"
                className="flex-1 text-center py-2 text-sm font-semibold rounded-md transition-all duration-300 text-muted-foreground hover:text-foreground"
              >
                Đăng nhập
              </Link>
              <Link
                href="/sign-up"
                className="flex-1 text-center py-2 text-sm font-semibold rounded-md transition-all duration-300 bg-white text-shop_orange shadow-sm border border-gray-100"
              >
                Đăng ký
              </Link>
            </div>
          </div>

          <CardContent className="p-6 pt-4">
            <SignupForm onSuccess={handleSignupSuccess} />
          </CardContent>
        </Card>
      </Container>
    </div>
  )
}
