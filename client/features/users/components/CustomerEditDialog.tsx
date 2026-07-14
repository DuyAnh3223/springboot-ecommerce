"use client"

import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { UserResponse } from "../user.type"
import { updateUserAction } from "../actions"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"
import { Loader2, AlertCircle } from "lucide-react"

const schema = z.object({
  firstName: z.string().min(1, "Vui lòng nhập tên"),
  lastName: z.string().min(1, "Vui lòng nhập họ"),
  email: z.string().email("Email không hợp lệ").min(1, "Vui lòng nhập email"),
  phone: z.string().min(10, "Số điện thoại phải từ 10 ký số").optional().or(z.literal("")),
  password: z.string().optional().or(z.literal("")),
  roles: z.array(z.string()).min(1, "Vui lòng chọn ít nhất 1 quyền"),
});

type FormValues = z.infer<typeof schema>;

interface CustomerEditDialogProps {
  user: UserResponse | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export function CustomerEditDialog({
  user,
  open,
  onOpenChange,
  onSuccess,
}: CustomerEditDialogProps) {
  const [error, setError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema as any),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      phone: "",
      password: "",
      roles: ["USER"],
    },
  })

  // Synchronize state when user changes
  useEffect(() => {
    if (user) {
      reset({
        firstName: user.firstName || "",
        lastName: user.lastName || "",
        email: user.email || "",
        phone: user.phone || "",
        password: "",
        roles: user.roles?.map((r) => r.name) || ["USER"],
      })
      setError(null)
    }
  }, [user, reset])

  const selectedRoles = watch("roles") || []

  const handleRoleChange = (roleName: string, checked: boolean) => {
    if (checked) {
      setValue("roles", [...selectedRoles, roleName], { shouldValidate: true })
    } else {
      setValue(
        "roles",
        selectedRoles.filter((r) => r !== roleName),
        { shouldValidate: true }
      )
    }
  }

  const onSubmit = async (values: FormValues) => {
    if (!user) return
    setError(null)
    
    // Clean up password and phone if empty
    const payload = {
      firstName: values.firstName,
      lastName: values.lastName,
      email: values.email,
      phone: values.phone || undefined,
      roles: values.roles,
      ...(values.password ? { password: values.password } : {}),
    }

    const result = await updateUserAction(user.id, payload)
    
    if (result.error) {
      setError(result.error)
      return
    }

    onSuccess()
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle>Cập nhật khách hàng</DialogTitle>
          <DialogDescription>
            Thay đổi thông tin của tài khoản khách hàng này. Nhấn Lưu để hoàn tất.
          </DialogDescription>
        </DialogHeader>

        {error && (
          <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 border border-destructive/20 rounded-lg animate-in fade-in duration-200">
            <AlertCircle className="size-4 shrink-0" />
            <p className="font-medium">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-2">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label htmlFor="lastName">Họ</Label>
              <Input
                id="lastName"
                placeholder="Nguyễn"
                className="border-slate-200 focus-visible:ring-shop_orange/20"
                {...register("lastName")}
              />
              {errors.lastName?.message && (
                <p className="text-xs text-destructive">{errors.lastName.message}</p>
              )}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="firstName">Tên</Label>
              <Input
                id="firstName"
                placeholder="Văn A"
                className="border-slate-200 focus-visible:ring-shop_orange/20"
                {...register("firstName")}
              />
              {errors.firstName?.message && (
                <p className="text-xs text-destructive">{errors.firstName.message}</p>
              )}
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="example@domain.com"
              className="border-slate-200 focus-visible:ring-shop_orange/20"
              {...register("email")}
            />
            {errors.email?.message && (
              <p className="text-xs text-destructive">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="phone">Số điện thoại</Label>
            <Input
              id="phone"
              placeholder="0987654321"
              className="border-slate-200 focus-visible:ring-shop_orange/20"
              {...register("phone")}
            />
            {errors.phone?.message && (
              <p className="text-xs text-destructive">{errors.phone.message}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="password">Mật khẩu mới (Bỏ trống nếu không đổi)</Label>
            <Input
              id="password"
              type="password"
              placeholder="••••••••"
              className="border-slate-200 focus-visible:ring-shop_orange/20"
              {...register("password")}
            />
            {errors.password?.message && (
              <p className="text-xs text-destructive">{errors.password.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Vai trò / Quyền hạn</Label>
            <div className="flex gap-6 items-center">
              <label className="flex items-center gap-2 text-sm font-normal cursor-pointer">
                <Checkbox
                  id="role-user"
                  checked={selectedRoles.includes("USER")}
                  onCheckedChange={(checked) => handleRoleChange("USER", !!checked)}
                />
                USER
              </label>
              <label className="flex items-center gap-2 text-sm font-normal cursor-pointer">
                <Checkbox
                  id="role-admin"
                  checked={selectedRoles.includes("ADMIN")}
                  onCheckedChange={(checked) => handleRoleChange("ADMIN", !!checked)}
                />
                ADMIN
              </label>
            </div>
            {errors.roles?.message && (
              <p className="text-xs text-destructive">{errors.roles.message}</p>
            )}
          </div>

          <DialogFooter className="pt-2">
            <DialogClose render={<Button type="button" variant="outline" className="cursor-pointer" />}>
              Hủy
            </DialogClose>
            <Button
              type="submit"
              disabled={isSubmitting}
              className="bg-slate-900 hover:bg-slate-800 text-white cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang lưu...
                </>
              ) : (
                "Lưu thay đổi"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
