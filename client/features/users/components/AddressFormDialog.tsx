"use client";

import React, { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAsyncAction } from "@/hooks";
import { createAddressAction, updateAddressAction } from "../actions";
import { AddressResponse } from "../address.type";
import { addressSchema, AddressInput } from "../schemas/address.schema";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";

interface AddressFormDialogProps {
  isOpen: boolean;
  onClose: () => void;
  editingAddress: AddressResponse | null;
}

export default function AddressFormDialog({
  isOpen,
  onClose,
  editingAddress,
}: AddressFormDialogProps) {
  const isEdit = !!editingAddress;
  const { isLoading, error, run } = useAsyncAction();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<AddressInput>({
    resolver: zodResolver(addressSchema),
    defaultValues: {
      recipientName: "",
      phone: "",
      province: "",
      district: "",
      ward: "",
      streetAddress: "",
      country: "VN",
      isDefault: false,
    },
  });

  // Reset form when opening/closing or changing editing address
  useEffect(() => {
    if (isOpen) {
      if (editingAddress) {
        reset({
          recipientName: editingAddress.recipientName,
          phone: editingAddress.phone,
          province: editingAddress.province,
          district: editingAddress.district,
          ward: editingAddress.ward,
          streetAddress: editingAddress.streetAddress,
          country: editingAddress.country || "VN",
          isDefault: editingAddress.isDefault || false,
        });
      } else {
        reset({
          recipientName: "",
          phone: "",
          province: "",
          district: "",
          ward: "",
          streetAddress: "",
          country: "VN",
          isDefault: false,
        });
      }
    }
  }, [isOpen, editingAddress, reset]);

  const onSubmit = (data: AddressInput) => {
    run(async () => {
      let result;
      if (isEdit && editingAddress) {
        result = await updateAddressAction(editingAddress.id, data);
      } else {
        result = await createAddressAction(data);
      }

      if (result.error) {
        throw new Error(result.error);
      }

      onClose();
    });
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg bg-white p-6 rounded-xl border border-slate-100 shadow-lg outline-none">
        <DialogHeader>
          <DialogTitle className="text-lg font-bold text-slate-850">
            {isEdit ? "Cập nhật địa chỉ nhận hàng" : "Thêm địa chỉ nhận hàng mới"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-2">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-lg text-xs font-medium animate-in fade-in duration-200">
              {error}
            </div>
          )}

          {/* Họ tên người nhận */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">Họ và tên người nhận</label>
            <input
              type="text"
              {...register("recipientName")}
              className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
              placeholder="Ví dụ: Nguyễn Văn A"
            />
            {errors.recipientName && (
              <span className="text-xs text-rose-600 font-medium">
                {errors.recipientName.message}
              </span>
            )}
          </div>

          {/* Số điện thoại */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">Số điện thoại liên hệ</label>
            <input
              type="text"
              {...register("phone")}
              className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
              placeholder="Ví dụ: 0987654321"
            />
            {errors.phone && (
              <span className="text-xs text-rose-600 font-medium">{errors.phone.message}</span>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {/* Tỉnh / Thành phố */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">Tỉnh / Thành phố</label>
              <input
                type="text"
                {...register("province")}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
                placeholder="Tỉnh/Thành"
              />
              {errors.province && (
                <span className="text-xs text-rose-600 font-medium">{errors.province.message}</span>
              )}
            </div>

            {/* Quận / Huyện */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">Quận / Huyện</label>
              <input
                type="text"
                {...register("district")}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
                placeholder="Quận/Huyện"
              />
              {errors.district && (
                <span className="text-xs text-rose-600 font-medium">{errors.district.message}</span>
              )}
            </div>

            {/* Phường / Xã */}
            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold text-slate-700">Phường / Xã</label>
              <input
                type="text"
                {...register("ward")}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
                placeholder="Phường/Xã"
              />
              {errors.ward && (
                <span className="text-xs text-rose-600 font-medium">{errors.ward.message}</span>
              )}
            </div>
          </div>

          {/* Địa chỉ chi tiết */}
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate-700">
              Địa chỉ chi tiết (số nhà, tên đường...)
            </label>
            <input
              type="text"
              {...register("streetAddress")}
              className="w-full px-3 py-2 rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-shop_orange/20 focus:border-shop_orange text-sm transition-all"
              placeholder="Ví dụ: 123 Đường Lê Lợi"
            />
            {errors.streetAddress && (
              <span className="text-xs text-rose-600 font-medium">
                {errors.streetAddress.message}
              </span>
            )}
          </div>

          {/* Đặt làm mặc định */}
          <div className="flex items-center gap-2 pt-2">
            <input
              type="checkbox"
              id="isDefault"
              {...register("isDefault")}
              className="rounded border-slate-300 text-shop_orange focus:ring-shop_orange/20 cursor-pointer"
            />
            <label htmlFor="isDefault" className="text-sm text-slate-600 cursor-pointer select-none">
              Đặt làm địa chỉ nhận hàng mặc định
            </label>
          </div>

          <DialogFooter className="pt-4 flex justify-end gap-2 border-t border-slate-100 mt-6">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-semibold border border-slate-200 rounded-lg hover:bg-slate-50 cursor-pointer text-slate-600"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="px-4 py-2 text-sm font-semibold bg-shop_orange hover:bg-shop_orange/90 text-white rounded-lg cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center min-w-20"
            >
              {isLoading ? "Đang lưu..." : "Lưu"}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
