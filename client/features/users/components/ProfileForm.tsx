"use client";

import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useAsyncAction } from "@/hooks";
import { updateUserAction } from "../actions";
import { UserResponse } from "../user.type";

const profileSchema = z.object({
  lastName: z.string().min(1, "Vui lòng nhập họ"),
  firstName: z.string().min(1, "Vui lòng nhập tên"),
  phone: z
    .string()
    .min(10, "Số điện thoại phải có ít nhất 10 chữ số")
    .regex(/^[0-9]+$/, "Số điện thoại không hợp lệ"),
  email: z.string().email("Email không đúng định dạng"),
});

type ProfileInput = z.infer<typeof profileSchema>;

interface ProfileFormProps {
  initialUser: UserResponse;
}

export default function ProfileForm({ initialUser }: ProfileFormProps) {
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const { isLoading, error, run } = useAsyncAction();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileInput>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      lastName: initialUser.lastName || "",
      firstName: initialUser.firstName || "",
      phone: initialUser.phone || "",
      email: initialUser.email || "",
    },
  });

  const onSubmit = (data: ProfileInput) => {
    setSuccessMessage(null);
    run(async () => {
      const payload = {
        ...data,
        roles: initialUser.roles?.map((r) => r.name) || ["USER"],
      };
      const result = await updateUserAction(initialUser.id, payload);
      if (result.error) {
        throw new Error(result.error);
      }
      setSuccessMessage("Cập nhật thông tin tài khoản thành công!");
    });
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-155 p-6 md:p-8">
      <h2 className="text-xl font-bold text-slate-850 mb-6 pb-3 border-b border-slate-100">
        Thông tin cá nhân
      </h2>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {successMessage && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg text-sm font-medium animate-in fade-in duration-200">
            {successMessage}
          </div>
        )}

        {error && (
          <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-sm font-medium animate-in fade-in duration-200">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Họ */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold text-slate-700">Họ</label>
            <input
              type="text"
              {...register("lastName")}
              className="w-full px-4 py-2.5 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
              placeholder="Nhập họ của bạn"
            />
            {errors.lastName && (
              <span className="text-xs text-rose-600 font-medium">{errors.lastName.message}</span>
            )}
          </div>

          {/* Tên */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold text-slate-700">Tên</label>
            <input
              type="text"
              {...register("firstName")}
              className="w-full px-4 py-2.5 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
              placeholder="Nhập tên của bạn"
            />
            {errors.firstName && (
              <span className="text-xs text-rose-600 font-medium">{errors.firstName.message}</span>
            )}
          </div>

          {/* Số điện thoại */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold text-slate-700">Số điện thoại</label>
            <input
              type="text"
              {...register("phone")}
              className="w-full px-4 py-2.5 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
              placeholder="Nhập số điện thoại"
            />
            {errors.phone && (
              <span className="text-xs text-rose-600 font-medium">{errors.phone.message}</span>
            )}
          </div>

          {/* Email */}
          <div className="flex flex-col gap-2">
            <label className="text-sm font-semibold text-slate-700">Địa chỉ Email</label>
            <input
              type="email"
              {...register("email")}
              className="w-full px-4 py-2.5 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all bg-slate-50 text-slate-500 cursor-not-allowed"
              disabled
            />
            {errors.email && (
              <span className="text-xs text-rose-600 font-medium">{errors.email.message}</span>
            )}
          </div>
        </div>

        <div className="flex justify-end pt-4 border-t border-slate-100">
          <button
            type="submit"
            disabled={isLoading}
            className="px-6 py-2.5 rounded-lg bg-shop_orange hover:bg-shop_orange/90 text-white font-semibold text-sm transition-all cursor-pointer shadow-sm hover:shadow-md disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center min-w-32"
          >
            {isLoading ? "Đang xử lý..." : "Lưu thay đổi"}
          </button>
        </div>
      </form>
    </div>
  );
}
