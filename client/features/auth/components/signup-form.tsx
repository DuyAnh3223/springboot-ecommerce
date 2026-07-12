'use client'

import { useForm } from 'react-hook-form'
import { SignUpInput, signUpSchema } from '@/schemas/auth'
import { zodResolver } from '@hookform/resolvers/zod'
import { signUpAction } from '@/app/actions/auth'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { useState } from 'react'
import { Loader2, Eye, EyeOff, User, Lock, AlertCircle, Info, CheckCircle2 } from 'lucide-react'

interface SignupFormProps {
  onSuccess: (username: string) => void
}

export function SignupForm({ onSuccess }: SignupFormProps) {
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')

  const form = useForm<SignUpInput>({
    resolver: zodResolver(signUpSchema as any),
    defaultValues: {
      username: '',
      password: '',
      confirmPassword: '',
      firstName: '',
      lastName: '',
    },
  })

  const { register, handleSubmit, formState: { errors, isSubmitting }, setError } = form

  async function onSubmit(values: SignUpInput) {
    const result = await signUpAction(values)

    if (result?.error) {
      setError('root', { message: result.error })
      return
    }

    if (result?.success) {
      setSuccessMessage('Đăng ký tài khoản thành công! Đang chuyển sang màn hình đăng nhập...')
      setTimeout(() => {
        onSuccess(values.username)
      }, 2000)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      {successMessage && (
        <div className="flex items-center gap-2 p-3 text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg animate-in fade-in duration-200">
          <CheckCircle2 className="size-4 shrink-0 text-green-600" />
          <p>{successMessage}</p>
        </div>
      )}

      {errors.root?.message && (
        <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-200">
          <AlertCircle className="size-4 shrink-0" />
          <p>{errors.root.message}</p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="signup-lastname">Họ</Label>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              <User className="size-4" />
            </span>
            <Input
              id="signup-lastname"
              type="text"
              placeholder="Họ"
              className="pl-9 h-10 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
              {...register('lastName')}
            />
          </div>
          {errors.lastName?.message && (
            <p className="text-xs text-destructive mt-1">{errors.lastName.message}</p>
          )}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="signup-firstname">Tên</Label>
          <div className="relative">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              <User className="size-4" />
            </span>
            <Input
              id="signup-firstname"
              type="text"
              placeholder="Tên"
              className="pl-9 h-10 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
              {...register('firstName')}
            />
          </div>
          {errors.firstName?.message && (
            <p className="text-xs text-destructive mt-1">{errors.firstName.message}</p>
          )}
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="signup-username">Tài khoản</Label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
            <Info className="size-4" />
          </span>
          <Input
            id="signup-username"
            type="text"
            placeholder="Tài khoản đăng nhập"
            className="pl-9 h-10 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
            {...register('username')}
          />
        </div>
        {errors.username?.message && (
          <p className="text-xs text-destructive mt-1">{errors.username.message}</p>
        )}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="signup-password">Mật khẩu</Label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
            <Lock className="size-4" />
          </span>
          <Input
            id="signup-password"
            type={showPassword ? 'text' : 'password'}
            placeholder="Mật khẩu"
            className="pl-9 pr-10 h-10 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
          >
            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        {errors.password?.message && (
          <p className="text-xs text-destructive mt-1">{errors.password.message}</p>
        )}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="signup-confirm-password">Xác nhận mật khẩu</Label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
            <Lock className="size-4" />
          </span>
          <Input
            id="signup-confirm-password"
            type={showConfirmPassword ? 'text' : 'password'}
            placeholder="Nhập lại mật khẩu"
            className="pl-9 pr-10 h-10 focus-visible:ring-shop_orange/20 focus-visible:border-shop_orange"
            {...register('confirmPassword')}
          />
          <button
            type="button"
            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors cursor-pointer"
          >
            {showConfirmPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </button>
        </div>
        {errors.confirmPassword?.message && (
          <p className="text-xs text-destructive mt-1">{errors.confirmPassword.message}</p>
        )}
      </div>

      <Button
        type="submit"
        disabled={isSubmitting || !!successMessage}
        className="w-full h-10 bg-shop_orange hover:bg-shop_orange/90 text-white font-semibold transition-all hover:shadow-lg disabled:opacity-75 disabled:cursor-not-allowed cursor-pointer mt-2"
      >
        {isSubmitting ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
            Đang đăng ký...
          </>
        ) : (
          'Đăng ký tài khoản'
        )}
      </Button>
    </form>
  )
}
