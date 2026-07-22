"use client";

import React from "react";
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
                ward: address.ward,
                street: address.street,
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
        <div className="py-3.5 px-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 group transition-colors hover:bg-slate-50/70 rounded-lg -mx-3">
            {/* Thông tin địa chỉ */}
            <div className="space-y-1 text-sm flex-1 min-w-0">
                <div className="flex items-center gap-2.5 flex-wrap">
                    <span className="font-bold text-slate-850">{address.recipientName}</span>
                    <span className="text-slate-300 text-xs">|</span>
                    <span className="text-slate-600 font-medium">{address.phone}</span>
                    {address.isDefault && (
                        <span className="bg-shop_orange/10 text-shop_orange text-[11px] font-semibold px-2 py-0.5 rounded border border-shop_orange/20">
                            Mặc định
                        </span>
                    )}
                </div>
                <div className="text-slate-600 text-xs sm:text-sm leading-relaxed truncate sm:whitespace-normal">
                    {address.street}, {address.ward}, {address.province}
                </div>
            </div>

            {/* Các hành động */}
            <div className="flex items-center gap-2.5 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100 text-xs font-medium">
                <button
                    onClick={() => onEdit(address)}
                    disabled={isSettingDefault || isDeleting}
                    className="text-blue-600 hover:text-blue-700 font-semibold cursor-pointer disabled:opacity-50 transition-colors"
                >
                    Cập nhật
                </button>

                {!address.isDefault && (
                    <>
                        <span className="text-slate-300">|</span>
                        <button
                            onClick={handleDelete}
                            disabled={isSettingDefault || isDeleting}
                            className="text-rose-600 hover:text-rose-700 font-semibold cursor-pointer disabled:opacity-50 transition-colors"
                        >
                            {isDeleting ? "Đang xóa..." : "Xóa"}
                        </button>
                        <span className="text-slate-300">|</span>
                        <button
                            onClick={handleSetDefault}
                            disabled={isSettingDefault || isDeleting}
                            className="text-slate-600 hover:text-shop_orange cursor-pointer border border-slate-200 hover:border-shop_orange px-2.5 py-1 rounded transition-all text-xs disabled:opacity-50"
                        >
                            {isSettingDefault ? "Đang xử lý..." : "Thiết lập mặc định"}
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}
