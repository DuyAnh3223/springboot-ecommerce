"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { User, MapPin, ShoppingBag } from "lucide-react";
import { UserResponse } from "../user.type";

interface ProfileSidebarProps {
  user: UserResponse;
}

export default function ProfileSidebar({ user }: ProfileSidebarProps) {
  const pathname = usePathname();

  const displayName = user.firstName
    ? `${user.lastName || ""} ${user.firstName}`.trim()
    : user.username;

  const menuItems = [
    {
      label: "Thông tin cá nhân",
      href: "/profile",
      icon: User,
    },
    {
      label: "Sổ địa chỉ",
      href: "/profile/address",
      icon: MapPin,
    },
    {
      label: "Đơn hàng đã mua",
      href: "/profile/orders",
      icon: ShoppingBag,
    },
  ];

  return (
    <aside className="w-full md:w-64 bg-white rounded-xl shadow-sm border border-slate-155 p-6 flex flex-col gap-6">
      {/* User Card */}
      <div className="flex items-center gap-3 border-b border-slate-100 pb-5">
        <div className="size-10 rounded-full bg-shop_orange/10 flex items-center justify-center text-shop_orange font-bold text-lg">
          {user.firstName ? user.firstName[0].toUpperCase() : user.username[0].toUpperCase()}
        </div>
        <div className="flex flex-col min-w-0">
          <span className="font-semibold text-slate-850 truncate">{displayName}</span>
          <span className="text-xs text-slate-400 truncate">{user.email}</span>
        </div>
      </div>

      {/* Navigation Menu */}
      <nav className="flex flex-col gap-1">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all ${
                isActive
                  ? "bg-shop_orange/10 text-shop_orange"
                  : "text-slate-600 hover:bg-slate-50 hover:text-slate-800"
              }`}
            >
              <Icon className={`size-4 ${isActive ? "text-shop_orange" : "text-slate-400"}`} />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
