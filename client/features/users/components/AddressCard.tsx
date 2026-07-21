"use client";

import React from "react";
import { MapPin, Phone, User, Trash2, Edit } from "lucide-react";
import { AddressResponse } from "../address.type";
import { useAsyncAction } from "@/hooks";
import { deleteAddressAction, updateAddressAction } from "../actions";

interface AddressCardProps {
  address: AddressResponse;
  onEdit: (address: AddressResponse) => void;
}

export default function AddressCard({ address, onEdit }: AddressCardProps) {
  const { isLoading: isDeleting, run: runDelete } = useAsyncAction();
  const { isLoading: isSettingDefault, run: runSetDefault } = useAsyncAction();

  const handleSetDefault = () => {
    runSetDefault(async () => {
      const payload = {
        recipientName: address.recipientName,
        phone: address.phone,
        province: address.province,
        district: address.district,
        ward: address.ward,
        streetAddress: address.streetAddress,
        country: address.country || "VN",
        isDefault: true,
      };
      const result = await updateAddressAction(address.id, payload);
      if (result.error) {
        throw new Error(result.error);
      }
    });
  };

  const handleDelete = () => {
    if (confirm("Bạn có chắc chắn muốn xóa địa chỉ này?")) {
      runDelete(async () => {
        const result = await deleteAddressAction(address.id);
        if (result.error) {
          throw new Error(result.error);
        }
      });
    }
  };

  return (
    <div
      className={`bg-white rounded-xl p-5 border transition-all flex flex-col gap-4 shadow-xs relative ${
        address.isDefault ? "border-shop_orange ring-1 ring-shop_orange/10" : "border-slate-155"
      }`}
    >
      {/* Badge Mặc Định */}
      {address.isDefault && (
        <span className="absolute top-4 right-4 bg-shop_orange/10 text-shop_orange text-xs font-semibold px-2.5 py-1 rounded-full">
          Mặc định
        </span>
      )}

      {/* Thông tin chính */}
      <div className="space-y-2.5">
        {/* Người nhận */}
        <div className="flex items-center gap-2 text-slate-800 font-semibold text-sm">
          <User className="size-4 text-slate-400 shrink-0" />
          <span>{address.recipientName}</span>
        </div>

        {/* Số điện thoại */}
        <div className="flex items-center gap-2 text-slate-600 text-sm">
          <Phone className="size-4 text-slate-400 shrink-0" />
          <span>{address.phone}</span>
        </div>

        {/* Địa chỉ */}
        <div className="flex items-start gap-2 text-slate-600 text-sm">
          <MapPin className="size-4 text-slate-400 shrink-0 mt-0.5" />
          <span className="leading-relaxed">
            {address.streetAddress}, {address.ward}, {address.district}, {address.province}
          </span>
        </div>
      </div>

      {/* Hành động */}
      <div className="flex items-center justify-between border-t border-slate-100 pt-4 mt-1">
        {/* Nút đặt mặc định */}
        <div>
          {!address.isDefault ? (
            <button
              onClick={handleSetDefault}
              disabled={isSettingDefault || isDeleting}
              className="text-xs text-shop_orange font-semibold hover:underline cursor-pointer disabled:opacity-50"
            >
              {isSettingDefault ? "Đang thiết lập..." : "Thiết lập mặc định"}
            </button>
          ) : (
            <span className="text-xs text-emerald-600 font-semibold flex items-center gap-1">
              ✓ Địa chỉ mặc định
            </span>
          )}
        </div>

        {/* Nút Sửa & Xóa */}
        <div className="flex items-center gap-4">
          {/* Nút Sửa */}
          <button
            onClick={() => onEdit(address)}
            disabled={isSettingDefault || isDeleting}
            className="text-slate-500 hover:text-slate-850 font-semibold text-xs flex items-center gap-1 transition-colors cursor-pointer disabled:opacity-50"
          >
            <Edit className="size-3.5" />
            <span>Sửa</span>
          </button>

          {/* Nút Xóa */}
          {!address.isDefault && (
            <button
              onClick={handleDelete}
              disabled={isSettingDefault || isDeleting}
              className="text-rose-600 hover:text-rose-700 font-semibold text-xs flex items-center gap-1 transition-colors cursor-pointer disabled:opacity-50"
            >
              <Trash2 className="size-3.5" />
              <span>{isDeleting ? "Đang xóa..." : "Xóa"}</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
