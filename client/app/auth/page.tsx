'use client'

import React, { useState } from 'react'
import Container from '@/components/Container'
import Logo from '@/components/Logo'
import { SigninForm } from '@/features/auth/components/signin-form'
import { SignupForm } from '@/features/auth/components/signup-form'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'

export default function AuthPage() {
  const [activeTab, setActiveTab] = useState<'signin' | 'signup'>('signin')
  const [prefilledUsername, setPrefilledUsername] = useState('')

  const handleSignupSuccess = (username: string) => {
    setPrefilledUsername(username)
    setActiveTab('signin')
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
              {activeTab === 'signin' ? 'Chào mừng trở lại!' : 'Tạo tài khoản mới'}
            </CardTitle>
            <CardDescription className="text-sm text-lightColor mt-1">
              {activeTab === 'signin' 
                ? 'Đăng nhập vào hệ thống để bắt đầu mua sắm.' 
                : 'Đăng ký tài khoản để nhận nhiều ưu đãi hấp dẫn.'}
            </CardDescription>
          </CardHeader>

          {/* Custom Animated Tabs */}
          <div className="px-6 pb-2">
            <div className="flex border border-gray-100 p-1 bg-gray-50/50 rounded-lg">
              <button
                type="button"
                onClick={() => setActiveTab('signin')}
                className={cn(
                  "flex-1 text-center py-2 text-sm font-semibold rounded-md transition-all duration-300 cursor-pointer",
                  activeTab === 'signin'
                    ? "bg-white text-shop_dark_green shadow-sm border border-gray-100"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                Đăng nhập
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('signup')}
                className={cn(
                  "flex-1 text-center py-2 text-sm font-semibold rounded-md transition-all duration-300 cursor-pointer",
                  activeTab === 'signup'
                    ? "bg-white text-shop_orange shadow-sm border border-gray-100"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                Đăng ký
              </button>
            </div>
          </div>

          <CardContent className="p-6 pt-4">
            {activeTab === 'signin' ? (
              <SigninForm key={prefilledUsername} prefilledUsername={prefilledUsername} />
            ) : (
              <SignupForm onSuccess={handleSignupSuccess} />
            )}
          </CardContent>
        </Card>
      </Container>
    </div>
  )
}