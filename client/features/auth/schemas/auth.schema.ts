import { z } from 'zod';

export const signInSchema = z.object({
  username: z.string().min(1, 'Vui lòng nhập tài khoản'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
})

export type SignInInput = z.infer<typeof signInSchema>

export const signUpSchema = z.object({
  username: z.string().min(3, 'Tài khoản phải có ít nhất 3 ký tự'),
  password: z.string().min(3, 'Mật khẩu phải có ít nhất 3 ký tự'),
  confirmPassword: z.string().min(3, 'Xác nhận mật khẩu phải có ít nhất 3 ký tự'),
  firstName: z.string().min(1, 'Vui lòng nhập tên của bạn'),
  lastName: z.string().min(1, 'Vui lòng nhập họ của bạn'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword'],
})

export type SignUpInput = z.infer<typeof signUpSchema>