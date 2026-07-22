"use client";

import React, { useState } from "react";
import { Plus, Home } from "lucide-react";
import { AddressResponse } from "../address.type";
import AddressCard from "./AddressCard";
import AddressFormDialog from "./AddressFormDialog";

interface AddressListProps {
    addresses: AddressResponse[];
}

export default function AddressList({ addresses }: AddressListProps) {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingAddress, setEditingAddress] = useState<AddressResponse | null>(null);

    const handleEdit = (address: AddressResponse) => {
        setEditingAddress(address);
        setIsDialogOpen(true);
    };

    const handleCreateNew = () => {
        setEditingAddress(null);
        setIsDialogOpen(true);
    };

    const handleCloseDialog = () => {
        setIsDialogOpen(false);
        setEditingAddress(null);
    };

    return (
        <div className="bg-white rounded-xl shadow-sm border border-slate-155 p-5 md:p-6 flex flex-col gap-4">
            {/* Header */}
            <div className="flex items-center justify-between gap-4 pb-4 border-b border-slate-100">
                <div>
                    <h2 className="text-lg font-bold text-slate-850">Sổ địa chỉ nhận hàng</h2>
                    <p className="text-xs text-slate-500 mt-0.5">
                        Quản lý danh sách địa chỉ nhận hàng của bạn
                    </p>
                </div>
                <button
                    onClick={handleCreateNew}
                    className="flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-lg bg-shop_orange hover:bg-shop_orange/90 text-white font-semibold text-xs transition-all cursor-pointer shadow-xs shrink-0"
                >
                    <Plus className="size-4" />
                    <span>Thêm địa chỉ mới</span>
                </button>
            </div>

            {/* Danh sách địa chỉ phẳng (divide-y) */}
            {addresses.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-10 px-4 border border-dashed border-slate-200 rounded-lg text-slate-400 gap-2.5">
                    <div className="p-2.5 bg-slate-50 rounded-full text-slate-400 border border-slate-100">
                        <Home className="size-6 text-slate-350" />
                    </div>
                    <span className="text-xs font-medium">Bạn chưa lưu địa chỉ nhận hàng nào.</span>
                    <button
                        onClick={handleCreateNew}
                        className="text-xs text-shop_orange font-bold hover:underline cursor-pointer"
                    >
                        Thêm địa chỉ đầu tiên ngay
                    </button>
                </div>
            ) : (
                <div className="divide-y divide-slate-100">
                    {addresses.map((address) => (
                        <AddressCard key={address.id} address={address} onEdit={handleEdit} />
                    ))}
                </div>
            )}

            {/* Duy nhất 1 instance Form Dialog cho cả tạo mới & sửa */}
            <AddressFormDialog
                isOpen={isDialogOpen}
                onClose={handleCloseDialog}
                editingAddress={editingAddress}
            />
        </div>
    );
}
