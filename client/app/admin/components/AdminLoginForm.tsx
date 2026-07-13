'use client'

import { useForm } from 'react-hook-form'
import { useAuthStore } from "@/features/auth/stores/auth.store"
import { useRouter } from "next/navigation"
import { SignInInput, signInSchema } from '@/features/auth/schemas/auth.schema'
import { zodResolver } from '@hookform/resolvers/zod'
import { adminSignInAction } from '@/features/auth/actions'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { useState } from 'react'
import { Loader2, Eye, EyeOff, User, Lock, AlertCircle } from 'lucide-react'

export function AdminLoginForm() {
  const router = useRouter()
  const setUser = useAuthStore((s) => s.setUser)
  const [showPassword, setShowPassword] = useState(false)

  const form = useForm<SignInInput>({
    resolver: zodResolver(signInSchema as any),
    defaultValues: { username: '', password: '' },
  })

  const { register, handleSubmit, formState: { errors, isSubmitting }, setError } = form

  async function onSubmit(values: SignInInput) {
    const result = await adminSignInAction(values)

    if (result?.error) {
      setError('root', { message: result.error })
      return
    }

    if (result?.success && result.user) {
      setUser(result.user)
      router.push('/admin')
      router.refresh()
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
      {errors.root?.message && (
        <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-200">
          <AlertCircle className="size-4 shrink-0" />
          <p className="font-medium">{errors.root.message}</p>
        </div>
      )}

      <div className="space-y-1.5">
        <Label htmlFor="admin-username">Tài khoản</Label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <User className="size-4" />
          </span>
          <Input
            id="admin-username"
            type="text"
            placeholder="Nhập tài khoản Admin"
            className="pl-9 h-10 border-slate-200 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
            {...register('username')}
          />
        </div>
        {errors.username?.message && (
          <p className="text-xs text-destructive mt-1">{errors.username.message}</p>
        )}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="admin-password">Mật khẩu</Label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
            <Lock className="size-4" />
          </span>
          <Input
            id="admin-password"
            type={showPassword ? 'text' : 'password'}
            placeholder="Nhập mật khẩu"
            className="pl-9 pr-10 h-10 border-slate-200 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors cursor-pointer"
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        {errors.password?.message && (
          <p className="text-xs text-destructive mt-1">{errors.password.message}</p>
        )}
      </div>

      <Button
        type="submit"
        disabled={isSubmitting}
        className="w-full h-10 bg-slate-900 hover:bg-slate-800 text-white font-semibold transition-all hover:shadow-lg disabled:opacity-75 disabled:cursor-not-allowed cursor-pointer"
      >
        {isSubmitting ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Đang xác thực...
          </>
        ) : (
          'Đăng nhập quản trị'
        )}
      </Button>
    </form>
  )
}
