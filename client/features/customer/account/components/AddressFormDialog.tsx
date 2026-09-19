"use client";

import { useEffect } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useAsyncAction } from "@/shared/hooks";
import { createAddressAction, updateAddressAction } from "@/features/users/actions";
import type { AddressResponse } from "@/features/users/address.type";
import { addressSchema, type AddressInput } from "@/features/users/schemas/address.schema";
import { ShippingAddressFields } from "@/features/shipment/components/ShippingAddressFields";
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

const emptyAddress: AddressInput = {
  recipientName: "",
  phone: "",
  province: "",
  district: "",
  ward: "",
  street: "",
  ghnProvinceId: 0,
  ghnDistrictId: 0,
  ghnWardCode: "",
  country: "VN",
  isDefault: false,
};

export default function AddressFormDialog({
  isOpen,
  onClose,
  editingAddress,
}: AddressFormDialogProps) {
  const isEdit = Boolean(editingAddress);
  const { isLoading, error, run } = useAsyncAction();
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    control,
    formState: { errors },
  } = useForm<AddressInput>({
    resolver: zodResolver(addressSchema),
    defaultValues: emptyAddress,
  });
  const routeValues = useWatch({ control });

  useEffect(() => {
    if (!isOpen) return;
    reset(editingAddress ? {
      recipientName: editingAddress.recipientName,
      phone: editingAddress.phone,
      province: editingAddress.province,
      district: editingAddress.district || "",
      ward: editingAddress.ward,
      street: editingAddress.street,
      ghnProvinceId: editingAddress.ghnProvinceId ?? 0,
      ghnDistrictId: editingAddress.ghnDistrictId ?? 0,
      ghnWardCode: editingAddress.ghnWardCode || "",
      country: editingAddress.country || "VN",
      isDefault: Boolean(editingAddress.isDefault),
    } : emptyAddress);
  }, [editingAddress, isOpen, reset]);

  const onSubmit = (data: AddressInput) => {
    run(async () => {
      const result = isEdit && editingAddress
        ? await updateAddressAction(editingAddress.id, data)
        : await createAddressAction(data);
      if (result.error) throw new Error(result.error);
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
          {error && <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}
          {isEdit && (!editingAddress?.ghnProvinceId || !editingAddress?.ghnDistrictId || !editingAddress?.ghnWardCode) && (
            <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
              Địa chỉ cũ thiếu mã tuyến vận chuyển. Vui lòng chọn lại đủ tỉnh, quận/huyện và phường/xã trước khi lưu.
            </div>
          )}
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-1.5 text-xs font-semibold text-slate-700">
              Họ và tên người nhận
              <input {...register("recipientName")} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
              {errors.recipientName && <span className="font-medium text-rose-600">{errors.recipientName.message}</span>}
            </label>
            <label className="space-y-1.5 text-xs font-semibold text-slate-700">
              Số điện thoại
              <input {...register("phone")} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" />
              {errors.phone && <span className="font-medium text-rose-600">{errors.phone.message}</span>}
            </label>
          </div>
          <ShippingAddressFields
            provinceId={routeValues.ghnProvinceId}
            districtId={routeValues.ghnDistrictId}
            wardCode={routeValues.ghnWardCode}
            provinceError={errors.province?.message || errors.ghnProvinceId?.message}
            districtError={errors.district?.message || errors.ghnDistrictId?.message}
            wardError={errors.ward?.message || errors.ghnWardCode?.message}
            onChange={(selection) => {
              setValue("province", selection.province, { shouldValidate: true });
              setValue("district", selection.district, { shouldValidate: true });
              setValue("ward", selection.ward, { shouldValidate: true });
              setValue("ghnProvinceId", selection.provinceId ?? 0, { shouldValidate: true });
              setValue("ghnDistrictId", selection.districtId ?? 0, { shouldValidate: true });
              setValue("ghnWardCode", selection.wardCode, { shouldValidate: true });
            }}
          />
          <label className="space-y-1.5 text-xs font-semibold text-slate-700">
            Địa chỉ chi tiết
            <input {...register("street")} className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm" placeholder="Số nhà, tên đường..." />
            {errors.street && <span className="font-medium text-rose-600">{errors.street.message}</span>}
          </label>
          <label className="flex items-center gap-2 pt-2 text-sm text-slate-600">
            <input type="checkbox" {...register("isDefault")} /> Đặt làm địa chỉ nhận hàng mặc định
          </label>
          <DialogFooter className="border-t border-slate-100 pt-4">
            <button type="button" onClick={onClose} className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600">Hủy</button>
            <button type="submit" disabled={isLoading} className="rounded-lg bg-shop_orange px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">
              {isLoading ? "Đang lưu..." : "Lưu"}
            </button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
